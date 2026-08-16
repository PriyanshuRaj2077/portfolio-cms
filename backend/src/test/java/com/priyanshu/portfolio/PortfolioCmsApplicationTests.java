package com.priyanshu.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priyanshu.portfolio.config.AdminInitializer;
import com.priyanshu.portfolio.entity.*;
import com.priyanshu.portfolio.repository.AdminUserRepository;
import com.priyanshu.portfolio.repository.BlogPostRepository;
import com.priyanshu.portfolio.repository.ProfileRepository;
import com.priyanshu.portfolio.service.PublishService;
import com.priyanshu.portfolio.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.priyanshu.portfolio.repository.SectionRepository;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "admin.username=testadmin",
        "admin.initial-password=SecurePass123!"
})
@AutoConfigureMockMvc
class PortfolioCmsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PublishService publishService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession authenticatedSession;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure test admin exists
        if (adminUserRepository.count() == 0) {
            AdminUser admin = AdminUser.builder()
                    .username("testadmin")
                    .passwordHash(passwordEncoder.encode("SecurePass123!"))
                    .role("ROLE_ADMIN")
                    .build();
            adminUserRepository.save(admin);
        }

        // Perform login to establish authenticated session
        MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testadmin\",\"password\":\"SecurePass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();

        authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession();
        assertNotNull(authenticatedSession);
    }

    @Test
    @DisplayName("1. AdminInitializer: Missing credentials with 0 admins throws IllegalStateException")
    void testAdminInitializerFailsWhenNoAdminAndNoCredentials() {
        AdminUserRepository mockRepo = org.mockito.Mockito.mock(AdminUserRepository.class);
        PasswordEncoder mockEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        org.mockito.Mockito.when(mockRepo.count()).thenReturn(0L);

        AdminInitializer initializer = new AdminInitializer(mockRepo, mockEncoder, null, null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> initializer.run());
        assertTrue(ex.getMessage().contains("initial credentials were not provided"));
    }

    @Test
    @DisplayName("1b. AdminInitializer: Existing admin does not require credentials")
    void testAdminInitializerPassesWhenAdminAlreadyExists() {
        AdminUserRepository mockRepo = org.mockito.Mockito.mock(AdminUserRepository.class);
        PasswordEncoder mockEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        org.mockito.Mockito.when(mockRepo.count()).thenReturn(1L);

        AdminInitializer initializer = new AdminInitializer(mockRepo, mockEncoder, null, null);
        assertDoesNotThrow(() -> initializer.run());
    }

    @Test
    @DisplayName("1c. Stored admin password in DB is BCrypt hashed")
    void testStoredPasswordIsBcryptHashed() {
        AdminUser admin = adminUserRepository.findByUsername("testadmin").orElseThrow();
        assertTrue(admin.getPasswordHash().startsWith("$2a$") || admin.getPasswordHash().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("SecurePass123!", admin.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wrongpass", admin.getPasswordHash()));
    }

    @Test
    @DisplayName("2. CSRF: Mutating POST/PUT/DELETE without CSRF is rejected with 403 Forbidden")
    void testMutatingRequestsWithoutCsrfAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/profile")
                        .session(authenticatedSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ASTRA\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/sections/sec-test")
                        .session(authenticatedSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2b. CSRF: Mutating requests with CSRF succeed when authenticated")
    void testMutatingRequestsWithCsrfSucceed() throws Exception {
        mockMvc.perform(put("/api/admin/profile")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ASTRA TEST\",\"title\":\"Principal Systems Architect\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ASTRA TEST"));
    }

    @Test
    @DisplayName("2c. GET requests work normally with or without CSRF token")
    void testGetRequestsWorkNormally() throws Exception {
        mockMvc.perform(get("/api/admin/profile")
                        .session(authenticatedSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("11. CMS Editability: Full Profile, Sections, Projects, Blogs and Atomic Publish")
    void testCompleteCmsAndPublishingWorkflow() throws Exception {
        // 1. Update Profile
        mockMvc.perform(put("/api/admin/profile")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CYBER ARCHITECT\",\"title\":\"Systems Engineering\",\"bio\":\"Distributed systems researcher.\"}"))
                .andExpect(status().isOk());

        // 2. Save Section with Valid Single-Letter Nav
        mockMvc.perform(post("/api/admin/sections")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"sec-research\",\"title\":\"Research\",\"label\":\"07 // R&D\",\"type\":\"TEXT\",\"navLetter\":\"R\",\"order\":7,\"visible\":true}"))
                .andExpect(status().isOk());

        // 3. Save Project
        mockMvc.perform(post("/api/admin/projects")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Kernel Scheduler\",\"description\":\"Custom lightweight task scheduler\",\"repoUrl\":\"https://github.com/example/kernel\"}"))
                .andExpect(status().isOk());

        // 4. Save DRAFT Blog and PUBLISHED Blog
        mockMvc.perform(post("/api/admin/blogs")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"blog-draft\",\"title\":\"Unpublished Draft Article\",\"slug\":\"draft-article\",\"status\":\"DRAFT\",\"contentMarkdown\":\"This is a draft.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/blogs")
                        .session(authenticatedSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"blog-published\",\"title\":\"Published Architectural Deep Dive\",\"slug\":\"test-article\",\"status\":\"PUBLISHED\",\"contentMarkdown\":\"# Deep Dive\\n\\n**Content**\",\"date\":\"2026-08-16\"}"))
                .andExpect(status().isOk());

        // 5. Test Media Upload
        MockMultipartFile file = new MockMultipartFile("file", "test_diagram.png", "image/png", "sample_image_bytes".getBytes());
        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(file)
                        .session(authenticatedSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").exists());

        // 6. Execute Atomic Publish
        MvcResult publishResult = mockMvc.perform(post("/api/admin/publish")
                        .session(authenticatedSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();

        // 7. Verify Manifest file and published JSON files exist
        assertTrue(storageService.verifyFileExists("manifest.json"));

        // Verify published blogs only contain PUBLISHED blogs, not DRAFT
        byte[] publishedBlogsBytes = storageService.readFile("blogs.v" + publishService.publish().get("version") + ".json");
        assertNotNull(publishedBlogsBytes);
        String publishedBlogsJson = new String(publishedBlogsBytes);
        assertTrue(publishedBlogsJson.contains("test-article"));
        assertFalse(publishedBlogsJson.contains("draft-article"));
    }

    @Test
    @DisplayName("12. Section Ordering: Automatic reordering, duplicate prevention, and letter validation")
    void testSectionAutomaticReorderingAndValidation() throws Exception {
        // Clear sections for test
        sectionRepository.deleteAll();

        // 1. Create 4 visible sections: Achievements (1), Experience (2), Tech Stack (3), Projects (4)
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-ach\",\"title\":\"Achievements\",\"navLetter\":\"A\",\"order\":1,\"visible\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-exp\",\"title\":\"Experience\",\"navLetter\":\"E\",\"order\":2,\"visible\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-skills\",\"title\":\"Tech Stack\",\"navLetter\":\"S\",\"order\":3,\"visible\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-proj\",\"title\":\"Projects\",\"navLetter\":\"P\",\"order\":4,\"visible\":true}"))
                .andExpect(status().isOk());

        // 2. Move Projects to position 2
        // Expected result: 1 Achievements, 2 Projects, 3 Experience, 4 Tech Stack
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-proj\",\"title\":\"Projects\",\"navLetter\":\"P\",\"order\":2,\"visible\":true}"))
                .andExpect(status().isOk());

        List<SectionEntity> reordered = sectionRepository.findAllByOrderByOrderAsc();
        assertEquals(4, reordered.size());
        assertEquals("sec-ach", reordered.get(0).getId());
        assertEquals(1, reordered.get(0).getOrder());

        assertEquals("sec-proj", reordered.get(1).getId());
        assertEquals(2, reordered.get(1).getOrder());

        assertEquals("sec-exp", reordered.get(2).getId());
        assertEquals(3, reordered.get(2).getOrder());

        assertEquals("sec-skills", reordered.get(3).getId());
        assertEquals(4, reordered.get(3).getOrder());

        // 3. Reject invalid navigation letter (e.g. multi-letter or lowercase or non-alpha)
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Invalid Nav\",\"navLetter\":\"123\",\"order\":1,\"visible\":true}"))
                .andExpect(status().isBadRequest());

        // 4. Reject duplicate navigation letter among visible sections
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Duplicate A\",\"navLetter\":\"A\",\"order\":1,\"visible\":true}"))
                .andExpect(status().isBadRequest());

        // 5. Allow duplicate navigation letter if section is hidden (visible: false)
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-hidden-a\",\"title\":\"Hidden Duplicate A\",\"navLetter\":\"A\",\"order\":99,\"visible\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("13. Unique Section IDs: Collision prevention and ID preservation upon title edit")
    void testUniqueSectionIdGenerationAndPreservation() throws Exception {
        sectionRepository.deleteAll();

        // 1. Create first "Photography" section without specifying ID
        MvcResult res1 = mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Photography\",\"navLetter\":\"P\",\"order\":1,\"visible\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        SectionEntity sec1 = objectMapper.readValue(res1.getResponse().getContentAsString(), SectionEntity.class);
        assertEquals("sec-photography", sec1.getId());

        // 2. Create second "Photography" section without specifying ID (should not collide or overwrite)
        MvcResult res2 = mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Photography\",\"navLetter\":\"Q\",\"order\":2,\"visible\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        SectionEntity sec2 = objectMapper.readValue(res2.getResponse().getContentAsString(), SectionEntity.class);
        assertEquals("sec-photography-2", sec2.getId());

        // Verify both exist in DB with separate IDs
        assertEquals(2, sectionRepository.count());
        assertTrue(sectionRepository.existsById("sec-photography"));
        assertTrue(sectionRepository.existsById("sec-photography-2"));

        // 3. Edit first section's title to "Fine Art Photography" — ID must be preserved
        mockMvc.perform(post("/api/admin/sections").session(authenticatedSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"sec-photography\",\"title\":\"Fine Art Photography\",\"navLetter\":\"P\",\"order\":1,\"visible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sec-photography"))
                .andExpect(jsonPath("$.title").value("Fine Art Photography"));
    }

    @Test
    @DisplayName("14. Direct Blog Route: /blog/<slug> resolves SPA HTML and root assets without 404")
    void testDirectBlogRouteResolvesAndServesSpa() throws Exception {
        // 1. Direct blog route returns 200 OK and contains SPA container and base tag
        mockMvc.perform(get("/blog/test-article"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"article-view\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<base href=\"/\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/app.js")));

        // 2. Direct assets are accessible from root paths
        mockMvc.perform(get("/css/variables.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/data/published/default/manifest.json"))
                .andExpect(status().isOk());
    }
}
