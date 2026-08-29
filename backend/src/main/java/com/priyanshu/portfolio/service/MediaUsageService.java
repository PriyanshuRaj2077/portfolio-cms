package com.priyanshu.portfolio.service;

import com.priyanshu.portfolio.entity.BlogPostEntity;
import com.priyanshu.portfolio.entity.MediaEntity;
import com.priyanshu.portfolio.entity.ProfileEntity;
import com.priyanshu.portfolio.entity.ProjectEntity;
import com.priyanshu.portfolio.repository.BlogPostRepository;
import com.priyanshu.portfolio.repository.ProfileRepository;
import com.priyanshu.portfolio.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans all content entities that can reference media files and returns
 * a complete, live list of references for a given MediaEntity.
 *
 * Design: NO denormalized fields are stored on MediaEntity. Usage is always
 * computed dynamically from actual content, so it is never stale and correctly
 * handles a single media file being referenced in multiple places simultaneously.
 *
 * Reference sources checked:
 *   1. ProfileEntity.avatarUrl           — equality match on fileUrl
 *   2. BlogPostEntity.featuredImageUrl   — equality match on fileUrl
 *   3. BlogPostEntity.contentMarkdown    — substring match on fileUrl (catches inline ![alt](url))
 *   4. ProjectEntity.coverImage          — equality match on fileUrl
 */
@Service
public class MediaUsageService {

    private final ProfileRepository profileRepository;
    private final BlogPostRepository blogPostRepository;
    private final ProjectRepository projectRepository;

    public MediaUsageService(
            ProfileRepository profileRepository,
            BlogPostRepository blogPostRepository,
            ProjectRepository projectRepository
    ) {
        this.profileRepository = profileRepository;
        this.blogPostRepository = blogPostRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Find all content locations that reference the given media file.
     *
     * @param media the MediaEntity to check
     * @return a list of MediaUsageReference entries, one per reference found;
     *         empty list means the media file is unused and safe to delete.
     */
    public List<MediaUsageReference> findUsages(MediaEntity media) {
        List<MediaUsageReference> usages = new ArrayList<>();
        String fileUrl = media.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            return usages;
        }

        // 1. Profile avatar
        for (ProfileEntity profile : profileRepository.findAll()) {
            if (fileUrl.equals(profile.getAvatarUrl())) {
                usages.add(new MediaUsageReference("Profile", "Avatar"));
            }
        }

        // 2. Blog featured image & inline Markdown content
        for (BlogPostEntity blog : blogPostRepository.findAll()) {
            boolean usedAsFeatured = fileUrl.equals(blog.getFeaturedImageUrl());
            // Inline Markdown: URL is embedded verbatim as full canonical URL string
            boolean usedInMarkdown = blog.getContentMarkdown() != null
                    && blog.getContentMarkdown().contains(fileUrl);

            if (usedAsFeatured) {
                usages.add(new MediaUsageReference("Blog",
                        "Featured image: " + blog.getTitle()));
            }
            if (usedInMarkdown) {
                usages.add(new MediaUsageReference("Blog",
                        "Inline image in: " + blog.getTitle()));
            }
        }

        // 3. Project cover image
        for (ProjectEntity project : projectRepository.findAllByOrderBySortOrderAsc()) {
            if (fileUrl.equals(project.getCoverImage())) {
                usages.add(new MediaUsageReference("Project",
                        "Cover image: " + project.getTitle()));
            }
        }

        return usages;
    }
}
