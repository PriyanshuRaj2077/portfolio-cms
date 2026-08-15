/* ==========================================================================
   DYNAMIC SECTION RENDERER & SANITIZATION ENGINE
   Editorial Layout & Monochrome Icon Resolver
   ========================================================================== */

const Renderer = {
  /**
   * Client-side HTML sanitizer to prevent XSS
   */
  sanitizeHTML(dirty) {
    if (!dirty) return '';
    const temp = document.createElement('div');
    temp.textContent = dirty;
    return temp.innerHTML;
  },

  /**
   * Single-letter navigation letter resolver directly from CMS
   */
  getSectionNavLetter(section) {
    let raw = (section.navLetter || section.icon || '').trim();
    if (!raw && section.title) {
      raw = section.title.trim().charAt(0);
    }
    return (raw.charAt(0) || 'S').toUpperCase();
  },

  /**
   * Main section renderer router
   */
  renderSection(section, data) {
    const sectionElement = document.createElement('section');
    sectionElement.id = section.id || `sec-${section.order}`;
    sectionElement.className = 'section';

    let innerContent = '';

    switch (section.type) {
      case 'ACHIEVEMENTS':
        innerContent = this.renderAchievements(data.achievements || []);
        break;
      case 'TIMELINE':
        innerContent = this.renderTimeline(data.experience || []);
        break;
      case 'SKILLS':
        innerContent = this.renderSkills(data.skills || []);
        break;
      case 'PROJECTS':
        innerContent = this.renderProjects(data.projects || []);
        break;
      case 'BLOG':
        innerContent = this.renderBlogs(data.blogs || []);
        break;
      case 'CONTACT':
        innerContent = this.renderContact(data.profile || {});
        break;
      case 'TEXT':
        innerContent = this.renderText(section.contentData || section.description || '');
        break;
      case 'GALLERY':
        innerContent = this.renderGallery(section.contentData || []);
        break;
      default:
        innerContent = `<p>${this.sanitizeHTML(section.description || '')}</p>`;
    }

    const sectionNum = String(section.sortOrder || section.order || 1).padStart(2, '0');

    sectionElement.innerHTML = `
      <div class="container">
        <div class="section-header">
          <span class="section-label-number">${sectionNum}  ${this.sanitizeHTML(section.label || 'SECTION')}</span>
          <h2 class="section-title">${this.sanitizeHTML(section.title)}</h2>
          ${section.description ? `<p class="section-description">${this.sanitizeHTML(section.description)}</p>` : ''}
        </div>
        <div class="section-body">
          ${innerContent}
        </div>
      </div>
    `;

    return sectionElement;
  },

  // 1. Achievements Renderer
  renderAchievements(achievements) {
    if (!achievements || achievements.length === 0) return '<p>No achievements published yet.</p>';
    return `
      <div class="achievements-editorial-list">
        ${achievements.map(item => `
          <div class="achievement-editorial-item">
            <div class="achievement-editorial-metric">${this.sanitizeHTML(item.metric)}</div>
            <div class="achievement-editorial-title">${this.sanitizeHTML(item.title)}</div>
            <div class="achievement-editorial-desc">${this.sanitizeHTML(item.descText || item.desc || '')}</div>
          </div>
        `).join('')}
      </div>
    `;
  },

  // 2. Timeline / Experience Renderer
  renderTimeline(experience) {
    if (!experience || experience.length === 0) return '<p>No experience history published yet.</p>';
    return `
      <div class="experience-editorial-list">
        ${experience.map(item => {
          let highlights = [];
          if (Array.isArray(item.highlights)) {
            highlights = item.highlights;
          } else if (typeof item.highlightsJson === 'string') {
            try { highlights = JSON.parse(item.highlightsJson); } catch (e) { highlights = []; }
          }
          return `
            <div class="experience-editorial-item">
              <div class="experience-header">
                <div class="experience-role-company">
                  ${this.sanitizeHTML(item.role)} <span class="company">— ${this.sanitizeHTML(item.company)}</span>
                </div>
                <div class="experience-date">${this.sanitizeHTML(item.startDate || '')} — ${this.sanitizeHTML(item.endDate || 'Present')}</div>
              </div>
              ${item.description ? `<p class="experience-desc">${this.sanitizeHTML(item.description)}</p>` : ''}
              ${highlights.length ? `
                <ul class="experience-highlights">
                  ${highlights.map(h => `<li>${this.sanitizeHTML(h)}</li>`).join('')}
                </ul>
              ` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  },

  // 3. Skills / Tech Stack Renderer (Clean Typographic Grid - No Progress Bars!)
  renderSkills(skillsData) {
    if (!skillsData || skillsData.length === 0) return '<p>No tech stack published yet.</p>';
    return `
      <div class="techstack-editorial">
        ${skillsData.map(cat => `
          <div class="techstack-category">
            <h3 class="techstack-cat-title">${this.sanitizeHTML(cat.category)}</h3>
            <div class="techstack-items-grid">
              ${(cat.skills || []).map(s => `
                <span class="techstack-item">${this.sanitizeHTML(s.name)}</span>
              `).join('')}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  },

  // 4. Projects Renderer (Editorial List / Grid)
  renderProjects(projects) {
    if (!projects || projects.length === 0) return '<p>No projects published yet.</p>';
    return `
      <div class="projects-editorial-list">
        ${projects.map((p, idx) => {
          const num = String(idx + 1).padStart(2, '0');
          let tags = [];
          if (Array.isArray(p.tags)) {
            tags = p.tags;
          } else if (typeof p.tagsJson === 'string') {
            try { tags = JSON.parse(p.tagsJson); } catch (e) { tags = p.tagsJson.split(','); }
          }
          return `
            <div class="project-editorial-item">
              <div class="project-num">${num}</div>
              <div class="project-details">
                <div>
                  <a href="${this.sanitizeHTML(p.liveUrl || p.repoUrl || '#')}" target="_blank" rel="noopener" class="project-title-link">
                    ${this.sanitizeHTML(p.title)} ↗
                  </a>
                </div>
                <p class="project-summary-text">${this.sanitizeHTML(p.summary || p.description || '')}</p>
                ${tags.length ? `
                  <div class="project-tech-tags">
                    ${tags.map(t => `<span class="project-tech-tag">${this.sanitizeHTML(t.trim())}</span>`).join('')}
                  </div>
                ` : ''}
              </div>
              <div class="project-arrow-links">
                ${p.repoUrl ? `<a href="${this.sanitizeHTML(p.repoUrl)}" target="_blank" rel="noopener" class="project-action-link">GitHub</a>` : ''}
                ${p.liveUrl ? `<a href="${this.sanitizeHTML(p.liveUrl)}" target="_blank" rel="noopener" class="project-action-link">Live</a>` : ''}
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  },

  // 5. Blogs Renderer (Editorial List)
  renderBlogs(blogs) {
    if (!blogs || blogs.length === 0) return '<p>No articles published yet.</p>';
    return `
      <div class="blogs-editorial-list">
        ${blogs.map((b, idx) => {
          const num = String(idx + 1).padStart(2, '0');
          return `
            <div class="blog-editorial-item" data-blog-id="${this.sanitizeHTML(b.id || b.slug || idx)}">
              <div class="blog-num">${num}</div>
              <div class="blog-main">
                <div class="blog-editorial-title">${this.sanitizeHTML(b.title)}</div>
                <div class="blog-editorial-summary">${this.sanitizeHTML(b.summary || '')}</div>
              </div>
              <div class="blog-date-meta">
                ${this.sanitizeHTML(b.date || '')} ${b.readTime ? `• ${this.sanitizeHTML(b.readTime)}` : ''}
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  },

  // 6. Contact Renderer (Minimal Editorial List)
  renderContact(profile) {
    const social = profile.socialLinks || {};
    return `
      <div class="contact-editorial">
        <p class="contact-intro-text">${this.sanitizeHTML(profile.bio || 'Reach out directly for collaborations or technical discussions.')}</p>
        <div class="contact-links-list">
          ${profile.email ? `
            <a href="mailto:${this.sanitizeHTML(profile.email)}" class="contact-item-link">
              <span>Email</span>
              <span>${this.sanitizeHTML(profile.email)} ↗</span>
            </a>
          ` : ''}
          ${profile.githubUrl || social.github ? `
            <a href="${this.sanitizeHTML(profile.githubUrl || social.github)}" target="_blank" rel="noopener" class="contact-item-link">
              <span>GitHub</span>
              <span>github.com ↗</span>
            </a>
          ` : ''}
          ${profile.linkedinUrl || social.linkedin ? `
            <a href="${this.sanitizeHTML(profile.linkedinUrl || social.linkedin)}" target="_blank" rel="noopener" class="contact-item-link">
              <span>LinkedIn</span>
              <span>linkedin.com ↗</span>
            </a>
          ` : ''}
          ${profile.twitterUrl || social.twitter ? `
            <a href="${this.sanitizeHTML(profile.twitterUrl || social.twitter)}" target="_blank" rel="noopener" class="contact-item-link">
              <span>Twitter / X</span>
              <span>x.com ↗</span>
            </a>
          ` : ''}
        </div>
      </div>
    `;
  },

  // 7. Custom Text Section Renderer
  renderText(content) {
    return `<div class="editorial-text">${this.sanitizeHTML(content)}</div>`;
  },

  // 8. Gallery Renderer
  renderGallery(images) {
    if (!images || images.length === 0) return '<p>No gallery items published yet.</p>';
    return `<div class="techstack-items-grid">${images.map(img => `<span>${this.sanitizeHTML(img.caption || img.url)}</span>`).join('')}</div>`;
  }
};
