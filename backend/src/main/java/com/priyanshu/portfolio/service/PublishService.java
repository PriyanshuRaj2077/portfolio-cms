package com.priyanshu.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.priyanshu.portfolio.entity.*;
import com.priyanshu.portfolio.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class PublishService {

    private final StorageService storageService;
    private final ProfileRepository profileRepository;
    private final SectionRepository sectionRepository;
    private final AchievementRepository achievementRepository;
    private final ExperienceRepository experienceRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final BlogPostRepository blogPostRepository;
    private final ObjectMapper objectMapper;

    private int currentVersion = 1;

    public PublishService(
            StorageService storageService,
            ProfileRepository profileRepository,
            SectionRepository sectionRepository,
            AchievementRepository achievementRepository,
            ExperienceRepository experienceRepository,
            SkillRepository skillRepository,
            ProjectRepository projectRepository,
            BlogPostRepository blogPostRepository,
            ObjectMapper objectMapper
    ) {
        this.storageService = storageService;
        this.profileRepository = profileRepository;
        this.sectionRepository = sectionRepository;
        this.achievementRepository = achievementRepository;
        this.experienceRepository = experienceRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.blogPostRepository = blogPostRepository;
        this.objectMapper = objectMapper;
    }

    public synchronized Map<String, Object> publish() throws Exception {
        int newVersion = currentVersion + 1;
        String vStr = ".v" + newVersion + ".json";

        String profileFilename = "profile" + vStr;
        String sectionsFilename = "sections" + vStr;
        String achievementsFilename = "achievements" + vStr;
        String experienceFilename = "experience" + vStr;
        String skillsFilename = "skills" + vStr;
        String projectsFilename = "projects" + vStr;
        String blogsFilename = "blogs" + vStr;

        // Step 1: Collect Data from Repositories
        List<ProfileEntity> profiles = profileRepository.findAll();
        ProfileEntity profile = profiles.isEmpty() ? createDefaultProfile() : profiles.get(0);
        List<SectionEntity> sections = sectionRepository.findAllByOrderByOrderAsc();
        if (sections.isEmpty()) sections = createDefaultSections();
        List<AchievementEntity> achievements = achievementRepository.findAllByOrderBySortOrderAsc();
        List<ExperienceEntity> experience = experienceRepository.findAllByOrderBySortOrderAsc();
        List<SkillEntity> skills = skillRepository.findAllByOrderBySortOrderAsc();
        List<ProjectEntity> projects = projectRepository.findAllByOrderBySortOrderAsc();
        List<BlogPostEntity> blogs = blogPostRepository.findAllByStatus("PUBLISHED");

        Map<String, byte[]> filesToUpload = new LinkedHashMap<>();
        filesToUpload.put(profileFilename, objectMapper.writeValueAsBytes(profile));
        filesToUpload.put(sectionsFilename, objectMapper.writeValueAsBytes(sections));
        filesToUpload.put(achievementsFilename, objectMapper.writeValueAsBytes(achievements));
        filesToUpload.put(experienceFilename, objectMapper.writeValueAsBytes(experience));
        filesToUpload.put(skillsFilename, objectMapper.writeValueAsBytes(skills));
        filesToUpload.put(projectsFilename, objectMapper.writeValueAsBytes(projects));
        filesToUpload.put(blogsFilename, objectMapper.writeValueAsBytes(blogs));

        // Step 2: Upload ALL Versioned Content Files
        for (Map.Entry<String, byte[]> entry : filesToUpload.entrySet()) {
            storageService.saveFile(entry.getKey(), entry.getValue());
        }

        // Step 3: Verify ALL Uploads Succeeded
        List<String> failedUploads = new ArrayList<>();
        for (String filename : filesToUpload.keySet()) {
            if (!storageService.verifyFileExists(filename)) {
                failedUploads.add(filename);
            }
        }

        // Step 4: If ANY upload failed, ABORT and DO NOT update manifest.json
        if (!failedUploads.isEmpty()) {
            throw new IllegalStateException("Atomic Publish Aborted: Upload failed for files: " + failedUploads + ". Manifest retained at version v" + currentVersion);
        }

        // Step 5: If ALL uploads succeeded, update and upload manifest.json LAST
        ObjectNode manifestNode = objectMapper.createObjectNode();
        manifestNode.put("version", newVersion);
        manifestNode.put("updatedAt", Instant.now().toString());

        ObjectNode filesNode = manifestNode.putObject("files");
        filesNode.put("profile", profileFilename);
        filesNode.put("sections", sectionsFilename);
        filesNode.put("achievements", achievementsFilename);
        filesNode.put("experience", experienceFilename);
        filesNode.put("skills", skillsFilename);
        filesNode.put("projects", projectsFilename);
        filesNode.put("blogs", blogsFilename);

        storageService.saveFile("manifest.json", objectMapper.writeValueAsBytes(manifestNode));

        this.currentVersion = newVersion;

        Map<String, Object> response = new HashMap<>();
        response.put("version", newVersion);
        response.put("updatedAt", Instant.now().toString());
        response.put("message", "Publication successful. Manifest updated to version v" + newVersion);
        return response;
    }

    private ProfileEntity createDefaultProfile() {
        return ProfileEntity.builder()
                .name("Priyanshu")
                .title("Developer • Builder • Curious Mind")
                .bio("Building minimal, resilient software, high-performance web systems, and exploring curious technical ideas.")
                .location("Global / Remote")
                .email("contact@priyanshu.me")
                .githubUrl("https://github.com")
                .linkedinUrl("https://linkedin.com")
                .twitterUrl("https://x.com")
                .avatarUrl("assets/images/priyanshu_illustration.svg")
                .build();
    }

    private List<SectionEntity> createDefaultSections() {
        return Arrays.asList(
            SectionEntity.builder().id("sec-achievements").title("Key Achievements").label("01 // HIGHLIGHTS").type("ACHIEVEMENTS").order(1).visible(true).theme("default").build(),
            SectionEntity.builder().id("sec-experience").title("Work Experience").label("02 // TIMELINE").type("TIMELINE").order(2).visible(true).theme("purple").build(),
            SectionEntity.builder().id("sec-tech-stack").title("Tech Stack & Tools").label("03 // SKILLS").type("SKILLS").order(3).visible(true).theme("default").build(),
            SectionEntity.builder().id("sec-projects").title("Selected Projects").label("04 // WORK").type("PROJECTS").order(4).visible(true).theme("orange").build(),
            SectionEntity.builder().id("sec-blog").title("Writing & Thoughts").label("05 // JOURNAL").type("BLOG").order(5).visible(true).theme("default").build(),
            SectionEntity.builder().id("sec-contact").title("Get In Touch").label("06 // CONNECT").type("CONTACT").order(6).visible(true).theme("default").build()
        );
    }
}
