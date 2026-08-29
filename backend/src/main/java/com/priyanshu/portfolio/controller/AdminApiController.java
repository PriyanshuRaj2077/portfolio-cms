package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.entity.*;
import com.priyanshu.portfolio.repository.*;
import com.priyanshu.portfolio.service.MediaUsageReference;
import com.priyanshu.portfolio.service.MediaUsageService;
import com.priyanshu.portfolio.service.PublishService;
import com.priyanshu.portfolio.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final ProfileRepository profileRepository;
    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final AchievementRepository achievementRepository;
    private final BlogPostRepository blogPostRepository;
    private final MediaRepository mediaRepository;
    private final CommentRepository commentRepository;
    private final StorageService storageService;
    private final PublishService publishService;
    private final MediaUsageService mediaUsageService;

    public AdminApiController(
            ProfileRepository profileRepository,
            SectionRepository sectionRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ExperienceRepository experienceRepository,
            AchievementRepository achievementRepository,
            BlogPostRepository blogPostRepository,
            MediaRepository mediaRepository,
            CommentRepository commentRepository,
            StorageService storageService,
            PublishService publishService,
            MediaUsageService mediaUsageService
    ) {
        this.profileRepository = profileRepository;
        this.sectionRepository = sectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.achievementRepository = achievementRepository;
        this.blogPostRepository = blogPostRepository;
        this.mediaRepository = mediaRepository;
        this.commentRepository = commentRepository;
        this.storageService = storageService;
        this.publishService = publishService;
        this.mediaUsageService = mediaUsageService;
    }

    // Profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        List<ProfileEntity> list = profileRepository.findAll();
        return ResponseEntity.ok(list.isEmpty() ? new ProfileEntity() : list.get(0));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileEntity profile) {
        List<ProfileEntity> list = profileRepository.findAll();
        if (!list.isEmpty()) {
            ProfileEntity existing = list.get(0);
            if (profile.getId() == null) {
                profile.setId(existing.getId());
            }
        }
        return ResponseEntity.ok(profileRepository.save(profile));
    }

    // Helper to generate unique, deterministic, readable section IDs (e.g. sec-photography, sec-photography-2)
    private String generateUniqueSectionId(String title) {
        String baseSlug = (title != null && !title.isBlank())
                ? title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                : "section";
        if (baseSlug.isBlank()) {
            baseSlug = "section";
        }
        String candidateId = "sec-" + baseSlug;
        if (!sectionRepository.existsById(candidateId)) {
            return candidateId;
        }
        int counter = 2;
        while (sectionRepository.existsById(candidateId + "-" + counter)) {
            counter++;
        }
        return candidateId + "-" + counter;
    }

    // Sections
    @GetMapping("/sections")
    public ResponseEntity<List<SectionEntity>> getSections() {
        return ResponseEntity.ok(sectionRepository.findAllByOrderByOrderAsc());
    }

    @PostMapping("/sections")
    public ResponseEntity<?> saveSection(@RequestBody SectionEntity section) {
        // 1. Navigation Letter Resolution & Validation (exactly one uppercase alphabetic character [A-Z])
        String navLetter = section.getNavLetter() != null ? section.getNavLetter().trim().toUpperCase() : "";
        if (navLetter.isBlank() && section.getTitle() != null && !section.getTitle().isBlank()) {
            String alphaChars = section.getTitle().trim().replaceAll("[^a-zA-Z]", "");
            if (!alphaChars.isEmpty()) {
                navLetter = alphaChars.substring(0, 1).toUpperCase();
            }
        }

        if (!navLetter.matches("^[A-Z]$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Navigation letter must be exactly one uppercase letter [A-Z]."));
        }
        section.setNavLetter(navLetter);

        // 2. Assign unique ID if new (preserve existing ID if editing)
        if (section.getId() == null || section.getId().isBlank()) {
            section.setId(generateUniqueSectionId(section.getTitle()));
        }

        // 3. Duplicate Letter Validation among visible sections
        boolean isVisible = section.getVisible() != null && section.getVisible();
        if (isVisible) {
            List<SectionEntity> allSections = sectionRepository.findAll();
            for (SectionEntity existing : allSections) {
                if (!existing.getId().equals(section.getId()) &&
                    existing.getVisible() != null && existing.getVisible() &&
                    navLetter.equalsIgnoreCase(existing.getNavLetter())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Navigation letter " + navLetter + " is already assigned to another visible section."));
                }
            }
        }

        // 4. Automatic Section Reordering & Deterministic Positioning
        if (isVisible) {
            int targetOrder = (section.getOrder() != null && section.getOrder() >= 1) ? section.getOrder() : 1;

            // Get all other visible sections in ascending order
            List<SectionEntity> visibleSections = sectionRepository.findAllByOrderByOrderAsc().stream()
                    .filter(s -> !s.getId().equals(section.getId()) && s.getVisible() != null && s.getVisible())
                    .collect(java.util.stream.Collectors.toList());

            int insertIndex = Math.min(Math.max(0, targetOrder - 1), visibleSections.size());
            visibleSections.add(insertIndex, section);

            // Re-normalize orders sequentially 1..N
            for (int i = 0; i < visibleSections.size(); i++) {
                SectionEntity s = visibleSections.get(i);
                s.setOrder(i + 1);
                sectionRepository.save(s);
            }
            return ResponseEntity.ok(section);
        } else {
            // Hidden section
            if (section.getOrder() == null) {
                section.setOrder(99);
            }
            SectionEntity saved = sectionRepository.save(section);

            // Re-normalize remaining visible sections
            List<SectionEntity> remainingVisible = sectionRepository.findAllByOrderByOrderAsc().stream()
                    .filter(s -> !s.getId().equals(section.getId()) && s.getVisible() != null && s.getVisible())
                    .collect(java.util.stream.Collectors.toList());
            for (int i = 0; i < remainingVisible.size(); i++) {
                SectionEntity s = remainingVisible.get(i);
                s.setOrder(i + 1);
                sectionRepository.save(s);
            }
            return ResponseEntity.ok(saved);
        }
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<?> deleteSection(@PathVariable String id) {
        sectionRepository.deleteById(id);

        // Normalize remaining visible sections to contiguous 1..N order
        List<SectionEntity> remainingVisible = sectionRepository.findAllByOrderByOrderAsc().stream()
                .filter(s -> s.getVisible() != null && s.getVisible())
                .collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < remainingVisible.size(); i++) {
            SectionEntity s = remainingVisible.get(i);
            s.setOrder(i + 1);
            sectionRepository.save(s);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Projects
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectEntity>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectEntity> saveProject(@RequestBody ProjectEntity project) {
        if (project.getId() == null || project.getId().isBlank()) {
            project.setId("proj-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return ResponseEntity.ok(projectRepository.save(project));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id) {
        projectRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Skills
    @GetMapping("/skills")
    public ResponseEntity<List<SkillEntity>> getSkills() {
        return ResponseEntity.ok(skillRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillEntity> saveSkill(@RequestBody SkillEntity skill) {
        return ResponseEntity.ok(skillRepository.save(skill));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Long id) {
        skillRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Experience
    @GetMapping("/experience")
    public ResponseEntity<List<ExperienceEntity>> getExperience() {
        return ResponseEntity.ok(experienceRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping("/experience")
    public ResponseEntity<ExperienceEntity> saveExperience(@RequestBody ExperienceEntity exp) {
        if (exp.getId() == null || exp.getId().isBlank()) {
            exp.setId("exp-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return ResponseEntity.ok(experienceRepository.save(exp));
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<?> deleteExperience(@PathVariable String id) {
        experienceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Achievements
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementEntity>> getAchievements() {
        return ResponseEntity.ok(achievementRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping("/achievements")
    public ResponseEntity<AchievementEntity> saveAchievement(@RequestBody AchievementEntity ach) {
        if (ach.getId() == null || ach.getId().isBlank()) {
            ach.setId("ach-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return ResponseEntity.ok(achievementRepository.save(ach));
    }

    @DeleteMapping("/achievements/{id}")
    public ResponseEntity<?> deleteAchievement(@PathVariable String id) {
        achievementRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Blogs
    @GetMapping("/blogs")
    public ResponseEntity<List<BlogPostEntity>> getBlogs() {
        return ResponseEntity.ok(blogPostRepository.findAll());
    }

    @PostMapping("/blogs")
    public ResponseEntity<?> saveBlog(@RequestBody BlogPostEntity blog) {
        if (blog.getId() == null || blog.getId().isBlank()) {
            blog.setId("blog-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Normalize slug safely
        String normalizedSlug = (blog.getSlug() != null && !blog.getSlug().isBlank())
                ? blog.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                : "";

        if (normalizedSlug.isBlank()) {
            String titleSlug = (blog.getTitle() != null && !blog.getTitle().isBlank())
                    ? blog.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                    : "";
            normalizedSlug = titleSlug.isBlank() ? blog.getId() : titleSlug;
        }
        blog.setSlug(normalizedSlug);

        // Check if another blog already owns this slug
        Optional<BlogPostEntity> existingWithSlug = blogPostRepository.findBySlug(normalizedSlug);
        if (existingWithSlug.isPresent() && !existingWithSlug.get().getId().equals(blog.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Blog slug '" + normalizedSlug + "' is already assigned to another article."));
        }

        if (blog.getStatus() == null || blog.getStatus().isBlank()) {
            blog.setStatus("DRAFT");
        }
        return ResponseEntity.ok(blogPostRepository.save(blog));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable String id) {
        blogPostRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // -------------------------------------------------------------------------
    // Comment Management (Admin)
    // -------------------------------------------------------------------------

    @GetMapping("/comments")
    public ResponseEntity<List<CommentEntity>> getComments() {
        return ResponseEntity.ok(commentRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/comments/{id}/approve")
    public ResponseEntity<?> approveComment(@PathVariable String id) {
        Optional<CommentEntity> opt = commentRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CommentEntity comment = opt.get();
        comment.setStatus("APPROVED");
        commentRepository.save(comment);
        return ResponseEntity.ok(Map.of("success", true, "status", "APPROVED"));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable String id) {
        if (!commentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        commentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // -------------------------------------------------------------------------
    // Media Library — enriched with live usage info
    // -------------------------------------------------------------------------

    @GetMapping("/media")
    public ResponseEntity<List<Map<String, Object>>> getMedia() {
        List<MediaEntity> mediaList = mediaRepository.findAll();
        List<Map<String, Object>> enriched = mediaList.stream().map(m -> {
            List<String> usedIn = mediaUsageService.findUsages(m).stream()
                    .map(MediaUsageReference::toString)
                    .collect(Collectors.toList());
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", m.getId());
            dto.put("fileName", m.getFileName());
            dto.put("fileUrl", m.getFileUrl());
            dto.put("mimeType", m.getMimeType());
            dto.put("fileSize", m.getFileSize());
            dto.put("uploadedAt", m.getUploadedAt() != null ? m.getUploadedAt().toString() : null);
            dto.put("usedIn", usedIn);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(enriched);
    }

    @PostMapping("/media/upload")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot upload empty file"));
        }

        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.png";
            String cleanName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String storedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + cleanName;

            storageService.saveMedia(storedFileName, file.getBytes(), file.getContentType());
            String publicMediaUrl = storageService.getMediaUrl(storedFileName);

            MediaEntity media = MediaEntity.builder()
                    .fileName(storedFileName)
                    .fileUrl(publicMediaUrl)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            MediaEntity saved = mediaRepository.save(media);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/media/{id}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long id) {
        Optional<MediaEntity> mediaOpt = mediaRepository.findById(id);
        if (mediaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MediaEntity media = mediaOpt.get();

        // Check ALL content references before allowing deletion
        List<MediaUsageReference> usages = mediaUsageService.findUsages(media);
        if (!usages.isEmpty()) {
            List<String> usedInLabels = usages.stream()
                    .map(MediaUsageReference::toString)
                    .collect(Collectors.toList());
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("error", "Media is currently referenced and cannot be deleted. Remove all references first.");
            conflict.put("usedIn", usedInLabels);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict);
        }

        // No references — safe to delete physically and from DB
        try {
            storageService.deleteMedia(media.getFileName());
        } catch (Exception e) {
            // Log but don't block DB record cleanup if storage delete fails
        }
        mediaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ATOMIC PUBLISH
    @PostMapping("/publish")
    public ResponseEntity<?> publish() {
        try {
            Map<String, Object> result = publishService.publish();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

