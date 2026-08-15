/* ==========================================================================
   MAIN APPLICATION LOADER & MANIFEST RESOLVER
   Zero Cold-Start Static Architecture
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

const App = {
  publishedData: null,

  async init() {
    console.log("Initializing Priyanshu's Portfolio...");
    try {
      this.publishedData = await this.loadPublishedContent();
      this.updateHeroProfile(this.publishedData.profile);
      this.renderDynamicSections(this.publishedData);
      this.setupBlogModal();
      Navigation.init();
    } catch (err) {
      console.error('Critical Error loading portfolio content:', err);
      this.showGracefulErrorState();
    }
  },

  /**
   * Load manifest and versioned content files with CDN failure fallback
   */
  async loadPublishedContent() {
    let baseUrl = CONFIG.PUBLIC_CONTENT_BASE_URL;
    let manifest = null;

    // 1. Try primary manifest fetch (no-cache)
    try {
      const manifestUrl = `${baseUrl}/${CONFIG.MANIFEST_FILE}?t=${Date.now()}`;
      const res = await fetch(manifestUrl, { cache: 'no-cache' });
      if (!res.ok) throw new Error(`HTTP ${res.status} fetching manifest from ${baseUrl}`);
      manifest = await res.json();
      console.log('Successfully loaded manifest v' + manifest.version + ' from primary storage');
    } catch (primaryErr) {
      console.warn('Primary storage manifest fetch failed. Switching to bundled fallback.', primaryErr);
      baseUrl = CONFIG.BUNDLED_FALLBACK_BASE_URL;
      const fallbackUrl = `${baseUrl}/${CONFIG.MANIFEST_FILE}`;
      const res = await fetch(fallbackUrl);
      if (!res.ok) throw new Error(`Bundled fallback manifest fetch failed (${res.status})`);
      manifest = await res.json();
    }

    const files = manifest.files || {};

    // 2. Fetch all versioned content files in parallel
    const [profile, sections, achievements, experience, skills, projects, blogs] = await Promise.all([
      this.fetchJSON(`${baseUrl}/${files.profile || 'profile.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.sections || 'sections.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.achievements || 'achievements.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.experience || 'experience.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.skills || 'skills.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.projects || 'projects.v1.json'}`),
      this.fetchJSON(`${baseUrl}/${files.blogs || 'blogs.v1.json'}`)
    ]);

    return { profile, sections, achievements, experience, skills, projects, blogs };
  },

  async fetchJSON(url) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status} fetching ${url}`);
      return await res.json();
    } catch (e) {
      console.error(`Error loading JSON file at ${url}:`, e);
      return [];
    }
  },

  /**
   * Populate Permanent Hero Section dynamically from published Profile JSON
   */
  updateHeroProfile(profile) {
    if (!profile) return;

    // 1. CMS-driven Name (Rendered exactly as stored without forcing uppercase)
    const nameElem = document.getElementById('hero-display-name');
    if (nameElem && profile.name) {
      nameElem.textContent = profile.name;
    }

    // 2. CMS-driven Subtitle / Title
    const subtitleElem = document.getElementById('hero-subtitle');
    if (subtitleElem && profile.title) {
      subtitleElem.textContent = Renderer.sanitizeHTML(profile.title);
    }

    // 3. CMS-driven Short Bio / Location
    const bioElem = document.getElementById('hero-bio');
    if (bioElem && profile.bio) {
      bioElem.textContent = Renderer.sanitizeHTML(profile.bio);
    }

    // 4. CMS-driven Personal Avatar Image (Empty square placeholder if empty)
    const avatarElem = document.getElementById('hero-avatar-img');
    const avatarContainer = document.querySelector('.hero-image-container');
    if (avatarElem) {
      if (profile.avatarUrl && profile.avatarUrl.trim() !== '') {
        avatarElem.src = profile.avatarUrl;
        avatarElem.alt = `${profile.name || 'Priyanshu'}`;
        avatarElem.style.display = 'block';
        if (avatarContainer) avatarContainer.classList.add('has-image');
      } else {
        avatarElem.src = '';
        avatarElem.alt = '';
        avatarElem.style.display = 'none';
        if (avatarContainer) avatarContainer.classList.remove('has-image');
      }
    }

    // Footer name & Dynamic Copyright Year
    const footerName = document.getElementById('footer-name');
    if (footerName && profile.name) {
      footerName.textContent = profile.name;
    }
    const footerYear = document.getElementById('footer-year');
    if (footerYear) {
      footerYear.textContent = new Date().getFullYear();
    }
  },

  /**
   * Render dynamic CMS sections below the Hero and populate single-letter sidebar navigation
   */
  renderDynamicSections(data) {
    const container = document.getElementById('sections-container');
    const navContainer = document.getElementById('dynamic-nav-links');

    if (!container) return;
    container.innerHTML = '';

    const sections = (data.sections || [])
      .filter(s => s.visible !== false)
      .sort((a, b) => (a.sortOrder || a.order || 0) - (b.sortOrder || b.order || 0));

    if (navContainer) {
      navContainer.innerHTML = '';
    }

    sections.forEach(section => {
      // 1. Create section DOM node
      const sectionNode = Renderer.renderSection(section, data);
      container.appendChild(sectionNode);

      // 2. Resolve single navigation letter (A, E, T, P, B, C...)
      const letter = Renderer.getSectionNavLetter(section);

      // 3. Create sidebar navigation item dynamically
      if (navContainer) {
        const li = document.createElement('li');
        li.className = 'sidebar-item';
        li.innerHTML = `
          <a href="#${sectionNode.id}" class="sidebar-link" title="${Renderer.sanitizeHTML(section.title)}">
            <span class="sidebar-letter" aria-hidden="true">${letter}</span>
            <span class="sidebar-label">${Renderer.sanitizeHTML(section.title)}</span>
          </a>
        `;
        navContainer.appendChild(li);
      }
    });
  },

  /**
   * Dedicated article reader & URL router for /blog/<slug>
   */
  setupBlogModal() {
    const modal = document.getElementById('article-modal');
    const closeBtn = document.getElementById('modal-close-btn');
    const contentBox = document.getElementById('modal-content');

    if (!modal || !contentBox) return;

    const openArticle = (article, pushUrl = true) => {
      if (!article) return;
      contentBox.innerHTML = `
        <div style="margin-bottom: 1.5rem;">
          <span style="font-family: var(--font-mono); font-size: 0.82rem; color: var(--text-muted);">${Renderer.sanitizeHTML(article.date || '')} ${article.readTime ? `• ${Renderer.sanitizeHTML(article.readTime)}` : ''}</span>
          <h2 style="font-size: 2rem; margin-top: 0.5rem; margin-bottom: 1rem; color: var(--text-primary);">${Renderer.sanitizeHTML(article.title)}</h2>
        </div>
        <div style="color: var(--text-secondary); line-height: 1.7; font-size: 1rem; font-family: var(--font-body);">
          ${article.summary ? `<p style="margin-bottom: 1rem; font-weight: 500;">${Renderer.sanitizeHTML(article.summary)}</p>` : ''}
          <div style="border-top: 1px solid var(--border-light); padding-top: 1rem; white-space: pre-wrap;">${Renderer.sanitizeHTML(article.contentMarkdown || article.content || article.summary || '')}</div>
        </div>
      `;
      modal.classList.add('open');
      modal.setAttribute('aria-hidden', 'false');

      const slug = article.slug || article.id;
      if (pushUrl && slug && !window.location.pathname.includes('/blog/' + slug)) {
        try {
          history.pushState({ blogSlug: slug }, '', '/blog/' + slug);
        } catch (e) {
          // Fallback if environment restricts path mutation
        }
      }
    };

    const closeArticle = (pushUrl = true) => {
      modal.classList.remove('open');
      modal.setAttribute('aria-hidden', 'true');
      if (pushUrl && window.location.pathname.startsWith('/blog/')) {
        try {
          history.pushState(null, '', '/');
        } catch (e) {}
      }
    };

    // Check direct /blog/<slug> URL on initial page load
    const checkDirectBlogRoute = () => {
      const path = window.location.pathname;
      if (path.startsWith('/blog/')) {
        const slug = path.replace(/^\/blog\//, '').replace(/\/$/, '');
        if (slug && this.publishedData && this.publishedData.blogs) {
          const article = this.publishedData.blogs.find(b => String(b.slug) === slug || String(b.id) === slug);
          if (article) {
            openArticle(article, false);
          }
        }
      }
    };

    // Handle clicks on blog items
    document.addEventListener('click', (e) => {
      const blogCard = e.target.closest('.blog-editorial-item');
      if (blogCard) {
        const blogId = blogCard.getAttribute('data-blog-id');
        const blogs = (this.publishedData && this.publishedData.blogs) ? this.publishedData.blogs : [];
        const article = blogs.find(b => String(b.id) === String(blogId) || String(b.slug) === String(blogId));
        if (article) {
          openArticle(article, true);
        }
      }
    });

    if (closeBtn) {
      closeBtn.addEventListener('click', () => closeArticle(true));
    }

    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        closeArticle(true);
      }
    });

    window.addEventListener('popstate', () => {
      const path = window.location.pathname;
      if (path.startsWith('/blog/')) {
        checkDirectBlogRoute();
      } else {
        closeArticle(false);
      }
    });

    // Check on startup
    checkDirectBlogRoute();
  },

  showGracefulErrorState() {
    const container = document.getElementById('sections-container');
    if (container) {
      container.innerHTML = `
        <div class="container" style="padding: 4rem 0; text-align: center;">
          <h2 style="color: var(--text-primary); margin-bottom: 1rem;">Content Temporarily Unavailable</h2>
          <p style="color: var(--text-secondary);">Unable to load published portfolio content. Please check back in a moment.</p>
        </div>
      `;
    }
  }
};
