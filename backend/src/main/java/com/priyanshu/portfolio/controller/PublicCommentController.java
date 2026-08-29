package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.entity.BlogPostEntity;
import com.priyanshu.portfolio.entity.CommentEntity;
import com.priyanshu.portfolio.repository.BlogPostRepository;
import com.priyanshu.portfolio.repository.CommentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Public-facing comment API.
 *
 * POST /api/public/comments          — Submit a comment (returns submitter token once)
 * GET  /api/public/comments/{slug}   — Get APPROVED comments + optionally the submitter's own PENDING comment
 *
 * Security design:
 * - All input is validated and length-capped before persistence.
 * - HTML is escaped to prevent XSS — plain text is stored, rendered as text in the UI.
 * - A secure random submitter token (UUID) is issued at submission and returned ONCE.
 *   It is stored hashed... for simplicity, stored as UUID in DB (not guessable sequentially).
 * - The public GET endpoint accepts an optional ?token= param.
 *   If provided and it matches a PENDING comment, that comment is appended with pendingOwnComment=true.
 *   The endpoint NEVER returns all pending comments.
 * - Simple IP-based rate limiting: max 5 submissions per IP per 10 minutes.
 * - Duplicate submission guard: same IP + same articleId within 30 seconds is rejected.
 */
@RestController
@RequestMapping("/api/public")
public class PublicCommentController {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int RATE_LIMIT_MAX_SUBMISSIONS = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000L; // 10 minutes
    private static final long DUPLICATE_GUARD_MS = 30 * 1000L; // 30 seconds

    private final CommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;

    // IP → [submission timestamps] — in-memory, cleared on restart (acceptable for spam mitigation)
    private final ConcurrentHashMap<String, List<Long>> submissionLog = new ConcurrentHashMap<>();
    // IP+articleId → last submission timestamp for duplicate guard
    private final ConcurrentHashMap<String, Long> lastSubmissionKey = new ConcurrentHashMap<>();

    public PublicCommentController(
            CommentRepository commentRepository,
            BlogPostRepository blogPostRepository
    ) {
        this.commentRepository = commentRepository;
        this.blogPostRepository = blogPostRepository;
    }

    /**
     * Submit a new comment. Returns the submitterToken ONCE — client must store it in sessionStorage.
     */
    @PostMapping("/comments")
    public ResponseEntity<?> submitComment(
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        String ip = getClientIp(request);

        // --- Rate limiting ---
        long now = System.currentTimeMillis();
        submissionLog.compute(ip, (k, timestamps) -> {
            if (timestamps == null) timestamps = new ArrayList<>();
            timestamps.removeIf(t -> now - t > RATE_LIMIT_WINDOW_MS);
            return timestamps;
        });
        List<Long> ipLog = submissionLog.get(ip);
        if (ipLog != null && ipLog.size() >= RATE_LIMIT_MAX_SUBMISSIONS) {
            return ResponseEntity.status(429).body(Map.of("error",
                    "Too many submissions. Please wait a few minutes before commenting again."));
        }

        // --- Extract and validate fields ---
        String articleId = sanitizeText(body.get("articleId"));
        String authorName = sanitizeText(body.get("authorName"));
        String content = sanitizeText(body.get("content"));

        if (articleId == null || articleId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Article ID is required."));
        }
        if (authorName == null || authorName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required."));
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty."));
        }
        if (authorName.length() > MAX_NAME_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Name must be " + MAX_NAME_LENGTH + " characters or fewer."));
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Comment must be " + MAX_CONTENT_LENGTH + " characters or fewer."));
        }

        // --- Verify article exists and is PUBLISHED ---
        Optional<BlogPostEntity> articleOpt = blogPostRepository.findById(articleId);
        if (articleOpt.isEmpty() || !"PUBLISHED".equals(articleOpt.get().getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Article not found or not published."));
        }

        // --- Duplicate guard: same IP + same article within 30 seconds ---
        String dupKey = ip + ":" + articleId;
        Long lastSub = lastSubmissionKey.get(dupKey);
        if (lastSub != null && now - lastSub < DUPLICATE_GUARD_MS) {
            return ResponseEntity.status(429).body(Map.of("error",
                    "Please wait a moment before submitting another comment."));
        }

        // --- Create comment ---
        String submitterToken = UUID.randomUUID().toString();
        String commentId = "cmt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        CommentEntity comment = CommentEntity.builder()
                .id(commentId)
                .articleId(articleId)
                .authorName(authorName)
                .content(content)
                .status("PENDING")
                .submitterToken(submitterToken)
                .build();

        commentRepository.save(comment);

        // Record submission for rate limiting and duplicate guard
        submissionLog.computeIfAbsent(ip, k -> new ArrayList<>()).add(now);
        lastSubmissionKey.put(dupKey, now);

        // Return submitter token — ONLY time it is sent to the client
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("submitterToken", submitterToken);
        response.put("message", "Your comment has been submitted and is awaiting approval.");
        return ResponseEntity.ok(response);
    }

    /**
     * Get approved comments for a published article by its slug.
     * Optionally, if ?token=<submitterToken> is provided and matches a PENDING comment,
     * that comment is appended as a single-item list flagged with pendingOwnComment=true.
     * NEVER exposes all pending comments.
     */
    @GetMapping("/comments/{slug}")
    public ResponseEntity<?> getComments(
            @PathVariable String slug,
            @RequestParam(value = "token", required = false) String submitterToken
    ) {
        // Resolve article by slug
        Optional<BlogPostEntity> articleOpt = blogPostRepository.findBySlug(slug);
        if (articleOpt.isEmpty() || !"PUBLISHED".equals(articleOpt.get().getStatus())) {
            return ResponseEntity.ok(Map.of("comments", List.of(), "pendingOwnComment", null));
        }
        String articleId = articleOpt.get().getId();

        // Fetch approved comments (public)
        List<CommentEntity> approved = commentRepository
                .findByArticleIdAndStatusOrderByCreatedAtAsc(articleId, "APPROVED");

        List<Map<String, Object>> commentDtos = new ArrayList<>();
        for (CommentEntity c : approved) {
            commentDtos.add(buildCommentDto(c, false));
        }

        // Check for submitter's own pending comment (private, token-gated)
        Map<String, Object> pendingOwn = null;
        if (submitterToken != null && !submitterToken.isBlank()) {
            Optional<CommentEntity> pendingOpt = commentRepository.findBySubmitterToken(submitterToken.trim());
            if (pendingOpt.isPresent()) {
                CommentEntity pending = pendingOpt.get();
                // Only show if it belongs to this article and is still pending
                if (pending.getArticleId().equals(articleId) && "PENDING".equals(pending.getStatus())) {
                    pendingOwn = buildCommentDto(pending, true);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("comments", commentDtos);
        result.put("pendingOwnComment", pendingOwn); // null if none
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> buildCommentDto(CommentEntity c, boolean isPendingOwn) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId());
        dto.put("authorName", c.getAuthorName());
        dto.put("content", c.getContent());
        dto.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        if (isPendingOwn) {
            dto.put("pendingOwnComment", true);
        }
        // submitterToken is NEVER included in any GET response
        return dto;
    }

    /**
     * Sanitize text: strip leading/trailing whitespace, escape HTML special chars.
     * Comments are stored as plain escaped text and rendered as text — no HTML/JS executes.
     */
    private String sanitizeText(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        // HTML-escape to prevent XSS if ever rendered in a non-text context
        return trimmed
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
