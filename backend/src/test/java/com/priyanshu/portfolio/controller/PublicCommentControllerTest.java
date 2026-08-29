package com.priyanshu.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priyanshu.portfolio.entity.BlogPostEntity;
import com.priyanshu.portfolio.entity.CommentEntity;
import com.priyanshu.portfolio.repository.BlogPostRepository;
import com.priyanshu.portfolio.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "admin.username=testadmin",
        "admin.initial-password=SecurePass123!",
        "portfolio.publish.output-dir=target/test-data/published/default",
        "portfolio.media.output-dir=target/test-media"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PublicCommentController publicCommentController;

    private BlogPostEntity testArticle;

    @BeforeEach
    void setUp() {
        publicCommentController.resetRateLimits();
        commentRepository.deleteAll();
        blogPostRepository.deleteAll();

        testArticle = BlogPostEntity.builder()
                .id("blog-test-1")
                .title("Zero Cold-Start Architecture")
                .slug("zero-cold-start-architecture")
                .status("PUBLISHED")
                .date("2026-08-20")
                .readTime("5 min read")
                .summary("Architecture deep dive.")
                .contentMarkdown("## Architecture overview\n\n![Diagram](https://example.com/diagram.png)")
                .build();
        blogPostRepository.save(testArticle);
    }

    @Test
    @DisplayName("POST /api/public/comments allows visitor submission WITHOUT CSRF token (public endpoint)")
    void testPublicCommentSubmissionWithoutCsrf() throws Exception {
        Map<String, String> payload = Map.of(
                "articleId", "blog-test-1",
                "authorName", "Alex Reader",
                "content", "Excellent breakdown of the architecture!"
        );

        MvcResult result = mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.submitterToken").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn();

        // Verify comment is saved as PENDING in repository
        assertEquals(1, commentRepository.count());
        CommentEntity saved = commentRepository.findAll().get(0);
        assertEquals("PENDING", saved.getStatus());
        assertEquals("Alex Reader", saved.getAuthorName());
        assertEquals("Excellent breakdown of the architecture!", saved.getContent());
        assertEquals("blog-test-1", saved.getArticleId());
        assertNotNull(saved.getSubmitterToken());
    }

    @Test
    @DisplayName("POST /api/public/comments accepts article slug in place of ID")
    void testPublicCommentSubmissionWithSlug() throws Exception {
        Map<String, String> payload = Map.of(
                "articleId", "zero-cold-start-architecture",
                "authorName", "Dev Reviewer",
                "content", "Great article on static publishing!"
        );

        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.submitterToken").isNotEmpty());

        assertEquals(1, commentRepository.count());
        CommentEntity saved = commentRepository.findAll().get(0);
        assertEquals("blog-test-1", saved.getArticleId());
    }

    @Test
    @DisplayName("POST /api/public/comments rejects missing fields and oversized content")
    void testPublicCommentValidation() throws Exception {
        // Missing name
        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "articleId", "blog-test-1",
                                "authorName", "",
                                "content", "Some content"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required."));

        // Missing content
        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "articleId", "blog-test-1",
                                "authorName", "Alex",
                                "content", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Comment cannot be empty."));

        // Non-existent article
        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "articleId", "non-existent-article",
                                "authorName", "Alex",
                                "content", "Hello"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Article not found or not published."));
    }

    @Test
    @DisplayName("GET /api/public/comments/{slug} returns only APPROVED comments to general public")
    void testGetCommentsApprovedOnly() throws Exception {
        // 1 Approved comment
        CommentEntity approved = CommentEntity.builder()
                .id("cmt-approved-1")
                .articleId("blog-test-1")
                .authorName("Jane Doe")
                .content("Approved public comment")
                .status("APPROVED")
                .submitterToken("token-jane-123")
                .build();
        commentRepository.save(approved);

        // 1 Pending comment from another visitor
        CommentEntity pending = CommentEntity.builder()
                .id("cmt-pending-1")
                .articleId("blog-test-1")
                .authorName("Bob Visitor")
                .content("Awaiting moderation")
                .status("PENDING")
                .submitterToken("token-bob-456")
                .build();
        commentRepository.save(pending);

        // Request without token -> only approved returned, pendingOwnComment is null
        mockMvc.perform(get("/api/public/comments/zero-cold-start-architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].authorName").value("Jane Doe"))
                .andExpect(jsonPath("$.comments[0].content").value("Approved public comment"))
                .andExpect(jsonPath("$.pendingOwnComment").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/public/comments/{slug}?token=... includes submitter's own PENDING comment privately")
    void testGetCommentsWithSubmitterToken() throws Exception {
        CommentEntity pending = CommentEntity.builder()
                .id("cmt-pending-own")
                .articleId("blog-test-1")
                .authorName("Alex Submitter")
                .content("My pending comment")
                .status("PENDING")
                .submitterToken("secret-token-alex")
                .build();
        commentRepository.save(pending);

        // Submitter fetches comments with their token
        mockMvc.perform(get("/api/public/comments/zero-cold-start-architecture")
                        .param("token", "secret-token-alex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(0)))
                .andExpect(jsonPath("$.pendingOwnComment.content").value("My pending comment"))
                .andExpect(jsonPath("$.pendingOwnComment.authorName").value("Alex Submitter"))
                .andExpect(jsonPath("$.pendingOwnComment.pendingOwnComment").value(true));

        // Different visitor with wrong token -> pending comment is NOT visible
        mockMvc.perform(get("/api/public/comments/zero-cold-start-architecture")
                        .param("token", "different-visitor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(0)))
                .andExpect(jsonPath("$.pendingOwnComment").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/public/comments/{id} also resolves by article ID directly")
    void testGetCommentsByIdFallback() throws Exception {
        CommentEntity approved = CommentEntity.builder()
                .id("cmt-approved-2")
                .articleId("blog-test-1")
                .authorName("Sara Engineer")
                .content("Found via ID directly")
                .status("APPROVED")
                .submitterToken("token-sara")
                .build();
        commentRepository.save(approved);

        mockMvc.perform(get("/api/public/comments/blog-test-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].authorName").value("Sara Engineer"));
    }

    @Test
    @DisplayName("XSS input in comments is escaped upon submission")
    void testXssEscaping() throws Exception {
        Map<String, String> payload = Map.of(
                "articleId", "blog-test-1",
                "authorName", "<script>alert('xss')</script>",
                "content", "<img src=x onerror=alert(1)><strong>bold</strong>"
        );

        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        CommentEntity saved = commentRepository.findAll().get(0);
        assertFalse(saved.getAuthorName().contains("<script>"));
        assertTrue(saved.getAuthorName().contains("&lt;script&gt;"));
        assertFalse(saved.getContent().contains("<img"));
        assertTrue(saved.getContent().contains("&lt;img"));
    }

    @Test
    @DisplayName("Duplicate comment submission within 30 seconds from same IP is rejected with 429")
    void testDuplicateSubmissionGuard() throws Exception {
        Map<String, String> payload = Map.of(
                "articleId", "blog-test-1",
                "authorName", "Alex Reader",
                "content", "First comment."
        );

        // First submission succeeds
        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Immediate second submission for same article from same IP fails with 429
        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Please wait a moment before submitting another comment."));
    }

    @Test
    @DisplayName("Rate limiting rejects more than 5 submissions in window with 429")
    void testRateLimitEnforcement() throws Exception {
        for (int i = 1; i <= 5; i++) {
            // Create separate article for each so duplicate guard isn't tripped
            BlogPostEntity article = BlogPostEntity.builder()
                    .id("blog-rate-" + i)
                    .title("Article " + i)
                    .slug("article-" + i)
                    .status("PUBLISHED")
                    .build();
            blogPostRepository.save(article);

            Map<String, String> payload = Map.of(
                    "articleId", "blog-rate-" + i,
                    "authorName", "User " + i,
                    "content", "Comment " + i
            );

            mockMvc.perform(post("/api/public/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        // 6th submission from same IP trips the rate limit
        BlogPostEntity article6 = BlogPostEntity.builder()
                .id("blog-rate-6")
                .title("Article 6")
                .slug("article-6")
                .status("PUBLISHED")
                .build();
        blogPostRepository.save(article6);

        Map<String, String> payload6 = Map.of(
                "articleId", "blog-rate-6",
                "authorName", "User 6",
                "content", "Comment 6"
        );

        mockMvc.perform(post("/api/public/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload6)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many submissions. Please wait a few minutes before commenting again."));
    }
}
