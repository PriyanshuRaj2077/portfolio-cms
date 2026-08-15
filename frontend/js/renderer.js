/* ==========================================================================
   DYNAMIC SECTION RENDERER & SANITIZATION ENGINE
   ========================================================================== */

const Renderer = {
  /**
   * Simple client-side HTML sanitizer to prevent XSS attacks
   */
  sanitizeHTML(dirty) {
    if (!dirty) return '';
    const temp = document.createElement('div');
    temp.textContent = dirty;
    return temp.innerHTML;
  },

  /**
   * Main section renderer router
   */
  renderSection(section, data) {
    const sectionElement = document.createElement('section');
    sectionElement.id = section.id || `section-${section.order}`;
    sectionElement.className = `section ${section.theme ? 'theme-' + section.theme : 'theme-default'}`;

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
        innerContent = this.renderText(section.contentData || '');
        break;
      case 'GALLERY':
        innerContent = this.renderGallery(section.contentData || []);
        break;
      default:
        innerContent = `<p>${this.sanitizeHTML(section.description || '')}</p>`;
    }

    sectionElement.innerHTML = `
      <div class="container">
        <div class="section-header">
          ${section.label ? `<span class="section-label">${this.sanitizeHTML(section.label)}</span>` : ''}
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
      <div class="achievements-grid">
        ${achievements.map(item => `
          <div class="achievement-card">
            <div class="achievement-metric">${this.sanitizeHTML(item.metric)}</div>
            <div class="achievement-title">${this.sanitizeHTML(item.title)}</div>
            <div class="achievement-desc">${this.sanitizeHTML(item.desc)}</div>
          </div>
        `).join('')}
      </div>
    `;
  },

  // 2. Timeline / Experience Renderer
  renderTimeline(experience) {
    if (!experience || experience.length === 0) return '<p>No experience history published yet.</p>';
    return `
      <div class="timeline-list">
        ${experience.map(item => `
          <div class="timeline-item">
            <div class="timeline-header">
              <div>
                <div class="timeline-role">${this.sanitizeHTML(item.role)}</div>
                <div class="timeline-company">${this.sanitizeHTML(item.company)}</div>
              </div>
              <div class="timeline-date">${this.sanitizeHTML(item.startDate)} — ${this.sanitizeHTML(item.endDate)}</div>
            </div>
            <p class="timeline-desc">${this.sanitizeHTML(item.description)}</p>
            ${item.highlights && item.highlights.length ? `
              <ul class="timeline-highlights">
                ${item.highlights.map(h => `<li>${this.sanitizeHTML(h)}</li>`).join('')}
              </ul>
            ` : ''}
          </div>
        `).join('')}
      </div>
    `;
  },

  // 3. Skills Renderer
  renderSkills(skillsData) {
    if (!skillsData || skillsData.length === 0) return '<p>No skills published yet.</p>';
    return `
      <div class="skills-categories">
        ${skillsData.map(cat => `
          <div class="skills-category">
            <h3 class="skills-category-title">${this.sanitizeHTML(cat.category)}</h3>
            <div class="skills-grid">
              ${cat.skills.map(s => `
                <div class="skill-chip">
                  <span>${this.sanitizeHTML(s.name)}</span>
                  ${s.level ? `<span class="skill-level">${this.sanitizeHTML(s.level)}</span>` : ''}
                </div>
              `).join('')}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  },

  // 4. Projects Renderer
  renderProjects(projects) {
    if (!projects || projects.length === 0) return '<p>No projects published yet.</p>';
    return `
      <div class="projects-grid">
        ${projects.map(p => `
          <div class="project-card">
            ${p.coverImage ? `
              <div class="project-thumb">
                <img src="${this.sanitizeHTML(p.coverImage)}" alt="${this.sanitizeHTML(p.title)}" loading="lazy" />
              </div>
            ` : ''}
            <div class="project-body">
              ${p.tags && p.tags.length ? `
                <div class="project-tags">
                  ${p.tags.map(t => `<span class="tag">${this.sanitizeHTML(t)}</span>`).join('')}
                </div>
              ` : ''}
              <h3 class="project-title">${this.sanitizeHTML(p.title)}</h3>
              <p class="project-desc">${this.sanitizeHTML(p.summary)}</p>
              <div class="project-links">
                ${p.repoUrl ? `<a href="${this.sanitizeHTML(p.repoUrl)}" target="_blank" rel="noopener" class="project-link">GitHub ↗</a>` : ''}
                ${p.liveUrl ? `<a href="${this.sanitizeHTML(p.liveUrl)}" target="_blank" rel="noopener" class="project-link">Live Demo ↗</a>` : ''}
              </div>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  },

  // 5. Blogs Renderer
  renderBlogs(blogs) {
    if (!blogs || blogs.length === 0) return '<p>No articles published yet.</p>';
    return `
      <div class="blogs-grid">
        ${blogs.map(b => `
          <div class="blog-card">
            <div class="blog-meta">
              <span>${this.sanitizeHTML(b.date)}</span>
              <span>•</span>
              <span>${this.sanitizeHTML(b.readTime)}</span>
            </div>
            <h3 class="blog-title">${this.sanitizeHTML(b.title)}</h3>
            <p class="blog-summary">${this.sanitizeHTML(b.summary)}</p>
            ${b.tags && b.tags.length ? `
              <div class="project-tags">
                ${b.tags.map(t => `<span class="tag">${this.sanitizeHTML(t)}</span>`).join('')}
              </div>
            ` : ''}
          </div>
        `).join('')}
      </div>
    `;
  },

  // 6. Contact Renderer
  renderContact(profile) {
    const social = profile.socialLinks || {};
    return `
      <div class="contact-container">
        <div class="contact-text">
          <h3>Let's Build Something Together</h3>
          <p>${this.sanitizeHTML(profile.bio || 'Feel free to reach out for collaborations, architecture consulting, or just a quick chat.')}</p>
        </div>
        <div class="contact-links">
          ${profile.email ? `
            <a href="mailto:${this.sanitizeHTML(profile.email)}" class="contact-link-item">
              <span class="contact-icon">✉</span>
              <span>${this.sanitizeHTML(profile.email)}</span>
            </a>
          ` : ''}
          ${social.github ? `
            <a href="${this.sanitizeHTML(social.github)}" target="_blank" rel="noopener" class="contact-link-item">
              <span class="contact-icon">⚙</span>
              <span>GitHub Profile</span>
            </a>
          ` : ''}
          ${social.linkedin ? `
            <a href="${this.sanitizeHTML(social.linkedin)}" target="_blank" rel="noopener" class="contact-link-item">
              <span class="contact-icon">💼</span>
              <span>LinkedIn</span>
            </a>
          ` : ''}
          ${social.twitter ? `
            <a href="${this.sanitizeHTML(social.twitter)}" target="_blank" rel="noopener" class="contact-link-item">
              <span class="contact-icon">💬</span>
              <span>Twitter / X</span>
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
    if (!images || images.length === 0) return '<p>No gallery images published yet.</p>';
    return `
      <div class="projects-grid">
        ${images.map(img => `
          <div class="project-card">
            <div class="project-thumb">
              <img src="${this.sanitizeHTML(img.url)}" alt="${this.sanitizeHTML(img.caption || '')}" loading="lazy" />
            </div>
            ${img.caption ? `<div class="project-body"><p class="project-desc">${this.sanitizeHTML(img.caption)}</p></div>` : ''}
          </div>
        `).join('')}
      </div>
    `;
  }
};
