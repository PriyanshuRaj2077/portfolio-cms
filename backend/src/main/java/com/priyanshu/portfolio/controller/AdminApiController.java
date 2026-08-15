package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.entity.*;
import com.priyanshu.portfolio.repository.*;
import com.priyanshu.portfolio.service.PublishService;
import com.priyanshu.portfolio.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
    private final StorageService storageService;
    private final PublishService publishService;

    public AdminApiController(
            ProfileRepository profileRepository,
            SectionRepository sectionRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ExperienceRepository experienceRepository,
            AchievementRepository achievementRepository,
            BlogPostRepository blogPostRepository,
            MediaRepository mediaRepository,
            StorageService storageService,
            PublishService publishService
    ) {
        this.profileRepository = profileRepository;
        this.sectionRepository = sectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.achievementRepository = achievementRepository;
        this.blogPostRepository = blogPostRepository;
        this.mediaRepository = mediaRepository;
        this.storageService = storageService;
        this.publishService = publishService;
    }

    // Profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        List<ProfileEntity> list = profileRepository.findAll();
        return ResponseEntity.ok(list.isEmpty() ? new ProfileEntity() : list.get(0));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileEntity profile) {
        return ResponseEntity.ok(profileRepository.save(profile));
    }

    // Sections
    @GetMapping("/sections")
    public ResponseEntity<List<SectionEntity>> getSections() {
        return ResponseEntity.ok(sectionRepository.findAllByOrderByOrderAsc());
    }

    @PostMapping("/sections")
    public ResponseEntity<?> saveSection(@RequestBody SectionEntity section) {
        // 1. Navigation Letter Validation (exactly one uppercase alphabetic character [A-Z])
        String navLetter = section.getNavLetter() != null ? section.getNavLetter().trim().toUpperCase() : "";
        if (!navLetter.matches("^[A-Z]$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Navigation letter must be exactly one uppercase letter [A-Z]."));
        }
        section.setNavLetter(navLetter);

        // 2. Assign default ID if new
        if (section.getId() == null || section.getId().isBlank()) {
            String slug = section.getTitle() != null ? section.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") : "";
            section.setId("sec-" + (slug.isBlank() ? UUID.randomUUID().toString().substring(0, 8) : slug));
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

        return ResponseEntity.ok(sectionRepository.save(section));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<?> deleteSection(@PathVariable String id) {
        sectionRepository.deleteById(id);
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
        if (blog.getSlug() == null || blog.getSlug().isBlank()) {
            String generatedSlug = blog.getTitle() != null ? blog.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") : "";
            blog.setSlug(generatedSlug.isBlank() ? blog.getId() : generatedSlug);
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

    // Media Library
    @GetMapping("/media")
    public ResponseEntity<List<MediaEntity>> getMedia() {
        return ResponseEntity.ok(mediaRepository.findAll());
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

            storageService.saveMedia(storedFileName, file.getBytes());

            MediaEntity media = MediaEntity.builder()
                    .fileName(storedFileName)
                    .fileUrl("/media/" + storedFileName)
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
        if (mediaOpt.isPresent()) {
            try {
                storageService.deleteMedia(mediaOpt.get().getFileName());
            } catch (Exception ignored) {}
            mediaRepository.deleteById(id);
        }
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

