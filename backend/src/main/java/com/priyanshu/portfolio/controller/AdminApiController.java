package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.entity.*;
import com.priyanshu.portfolio.repository.*;
import com.priyanshu.portfolio.service.PublishService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final PublishService publishService;

    public AdminApiController(
            ProfileRepository profileRepository,
            SectionRepository sectionRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository,
            ExperienceRepository experienceRepository,
            AchievementRepository achievementRepository,
            BlogPostRepository blogPostRepository,
            PublishService publishService
    ) {
        this.profileRepository = profileRepository;
        this.sectionRepository = sectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.achievementRepository = achievementRepository;
        this.blogPostRepository = blogPostRepository;
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
    public ResponseEntity<SectionEntity> saveSection(@RequestBody SectionEntity section) {
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
    public ResponseEntity<BlogPostEntity> saveBlog(@RequestBody BlogPostEntity blog) {
        return ResponseEntity.ok(blogPostRepository.save(blog));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable String id) {
        blogPostRepository.deleteById(id);
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
