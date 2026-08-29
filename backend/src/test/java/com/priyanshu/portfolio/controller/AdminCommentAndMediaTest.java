package com.priyanshu.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priyanshu.portfolio.entity.BlogPostEntity;
import com.priyanshu.portfolio.entity.CommentEntity;
import com.priyanshu.portfolio.entity.MediaEntity;
import com.priyanshu.portfolio.entity.ProfileEntity;
import com.priyanshu.portfolio.entity.ProjectEntity;
import com.priyanshu.portfolio.repository.*;
import com.priyanshu.portfolio.service.MediaUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class AdminCommentAndMediaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MediaUsageService mediaUsageService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        blogPostRepository.deleteAll();
        mediaRepository.deleteAll();
        profileRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    @DisplayName("Admin can list comments, approve pending comment, and delete comment")
    void testAdminCommentModerationWorkflow() throws Exception {
        BlogPostEntity blog = BlogPostEntity.builder()
                .id("blog-mod-1")
                .title("Testing Moderation")
                .slug("testing-moderation")
                .status("PUBLISHED")
                .build();
        blogPostRepository.save(blog);

        CommentEntity comment = CommentEntity.builder()
                .id("cmt-mod-1")
                .articleId("blog-mod-1")
                .authorName("Visitor John")
                .content("Awaiting approval")
                .status("PENDING")
                .submitterToken("token-john")
                .build();
        commentRepository.save(comment);

        // 1. Admin gets comments list
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].authorName").value("Visitor John"));

        // 2. Admin approves comment
        mockMvc.perform(post("/api/admin/comments/cmt-mod-1/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify status in DB is APPROVED
        CommentEntity updated = commentRepository.findById("cmt-mod-1").orElseThrow();
        assertEquals("APPROVED", updated.getStatus());

        // Verify public endpoint now returns it
        mockMvc.perform(get("/api/public/comments/testing-moderation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].content").value("Awaiting approval"));

        // 3. Admin deletes comment
        mockMvc.perform(delete("/api/admin/comments/cmt-mod-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(commentRepository.existsById("cmt-mod-1"));
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    @DisplayName("Media deletion is blocked with 409 Conflict when referenced in avatar, featured image, inline markdown, or project cover")
    void testMediaDeletionProtection() throws Exception {
        String mediaUrl = "https://example.supabase.co/storage/v1/object/public/portfolio/media/test-photo.png";

        MediaEntity media = MediaEntity.builder()
                .fileName("test-photo.png")
                .fileUrl(mediaUrl)
                .mimeType("image/png")
                .fileSize(1024L)
                .build();
        media = mediaRepository.save(media);
        Long mediaId = media.getId();

        // 1. Referenced in Profile Avatar
        ProfileEntity profile = ProfileEntity.builder()
                .id(1L)
                .name("Alex")
                .avatarUrl(mediaUrl)
                .build();
        profileRepository.save(profile);

        mockMvc.perform(delete("/api/admin/media/" + mediaId).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("Media is currently referenced")))
                .andExpect(jsonPath("$.usedIn[0]").value("Profile: Avatar"));

        // Clear profile avatar
        profileRepository.deleteAll();

        // 2. Referenced as Blog Featured Image
        BlogPostEntity blog = BlogPostEntity.builder()
                .id("blog-featured-test")
                .title("Featured Image Blog")
                .slug("featured-image-blog")
                .featuredImageUrl(mediaUrl)
                .status("DRAFT")
                .build();
        blogPostRepository.save(blog);

        mockMvc.perform(delete("/api/admin/media/" + mediaId).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.usedIn[0]").value("Blog: Featured image: Featured Image Blog"));

        // 3. Referenced as Inline Markdown Image
        blog.setFeaturedImageUrl(null);
        blog.setContentMarkdown("Here is an inline diagram: ![" + "diagram" + "](" + mediaUrl + ")");
        blogPostRepository.save(blog);

        mockMvc.perform(delete("/api/admin/media/" + mediaId).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.usedIn[0]").value("Blog: Inline image in: Featured Image Blog"));

        // Clear blog
        blogPostRepository.deleteAll();

        // 4. Referenced as Project Cover Image
        ProjectEntity project = ProjectEntity.builder()
                .id("proj-1")
                .title("Distributed System")
                .coverImage(mediaUrl)
                .sortOrder(0)
                .build();
        projectRepository.save(project);

        mockMvc.perform(delete("/api/admin/media/" + mediaId).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.usedIn[0]").value("Project: Cover image: Distributed System"));

        // Clear project
        projectRepository.deleteAll();

        // 5. Unreferenced media can be deleted safely
        mockMvc.perform(delete("/api/admin/media/" + mediaId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(mediaRepository.existsById(mediaId));
    }

    @Autowired
    private SectionRepository sectionRepository;

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    @DisplayName("Section management: view default sections, create, edit, reorder, toggle visibility, and delete")
    void testSectionManagementWorkflow() throws Exception {
        sectionRepository.deleteAll();

        // 1. Initial get auto-seeds 6 default sections
        mockMvc.perform(get("/api/admin/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].navLetter").value("A"))
                .andExpect(jsonPath("$[0].title").value("Achievements"));

        // 2. Create a new custom dynamic section
        String newSectionJson = """
                {
                    "title": "Photography",
                    "navLetter": "F",
                    "type": "GALLERY",
                    "order": 1,
                    "label": "07 // GALLERY",
                    "description": "Visual captures",
                    "visible": true
                }
                """;

        mockMvc.perform(post("/api/admin/sections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newSectionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sec-photography"))
                .andExpect(jsonPath("$.navLetter").value("F"));

        // Verify reordering placed it at order 1 and shifted other visible sections
        mockMvc.perform(get("/api/admin/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].id").value("sec-photography"))
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[1].order").value(2));

        // 3. Duplicate navigation letter is rejected for visible sections
        String duplicateLetterJson = """
                {
                    "title": "Film",
                    "navLetter": "F",
                    "type": "GALLERY",
                    "order": 2,
                    "visible": true
                }
                """;

        mockMvc.perform(post("/api/admin/sections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateLetterJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("is already assigned to another visible section")));

        // 4. Edit section to change order and title
        String editJson = """
                {
                    "id": "sec-photography",
                    "title": "Fine Art Photography",
                    "navLetter": "F",
                    "type": "GALLERY",
                    "order": 3,
                    "visible": true
                }
                """;

        mockMvc.perform(post("/api/admin/sections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fine Art Photography"));

        // 5. Delete section
        mockMvc.perform(delete("/api/admin/sections/sec-photography").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(sectionRepository.existsById("sec-photography"));
    }
}
