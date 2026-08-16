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
    console.log("Initializing Portfolio...");
    try {
      this.publishedData = await this.loadPublishedContent();
      this.updateHeroProfile(this.publishedData.profile);
      this.updateHeroActions(this.publishedData.sections);
      this.renderDynamicSections(this.publishedData);
      this.setupBlogRouter();
      Navigation.init();
    } catch (err) {
      console.error('Critical Error loading portfolio content:', err);
      this.showGracefulErrorState();
    }
  },

  /**
   * Load manifest and versioned content files with per-file fallback architecture
   */
  async loadPublishedContent() {
    const primaryBase = CONFIG.PUBLIC_CONTENT_BASE_URL || '/data/published/default';
    const fallbackBase = CONFIG.BUNDLED_FALLBACK_BASE_URL || '/data/published/default';
    let manifest = null;

    // 1. Try primary manifest fetch (no-cache)
    try {
      const manifestUrl = `${primaryBase}/${CONFIG.MANIFEST_FILE}?t=${Date.now()}`;
      const res = await fetch(manifestUrl, { cache: 'no-cache' });
      if (!res.ok) throw new Error(`HTTP ${res.status} fetching manifest from ${primaryBase}`);
      manifest = await res.json();
      console.log('Successfully loaded manifest v' + manifest.version + ' from primary storage');
    } catch (primaryErr) {
      console.warn('Primary storage manifest fetch failed. Switching to bundled fallback manifest.', primaryErr);
      try {
        const fallbackUrl = `${fallbackBase}/${CONFIG.MANIFEST_FILE}`;
        const res = await fetch(fallbackUrl);
        if (!res.ok) throw new Error(`Bundled fallback manifest fetch failed (${res.status})`);
        manifest = await res.json();
      } catch (fallbackErr) {
        console.error('All manifest sources failed:', fallbackErr);
        manifest = { files: {} };
      }
    }

    const files = (manifest && manifest.files) ? manifest.files : {};

    // 2. Fetch all versioned content files in parallel with per-file fallback
    const [profile, sections, achievements, experience, skills, projects, blogs] = await Promise.all([
      this.fetchWithFallback(`${primaryBase}/${files.profile || 'profile.v1.json'}`, `${fallbackBase}/profile.v1.json`, {}),
      this.fetchWithFallback(`${primaryBase}/${files.sections || 'sections.v1.json'}`, `${fallbackBase}/sections.v1.json`, []),
      this.fetchWithFallback(`${primaryBase}/${files.achievements || 'achievements.v1.json'}`, `${fallbackBase}/achievements.v1.json`, []),
      this.fetchWithFallback(`${primaryBase}/${files.experience || 'experience.v1.json'}`, `${fallbackBase}/experience.v1.json`, []),
      this.fetchWithFallback(`${primaryBase}/${files.skills || 'skills.v1.json'}`, `${fallbackBase}/skills.v1.json`, []),
      this.fetchWithFallback(`${primaryBase}/${files.projects || 'projects.v1.json'}`, `${fallbackBase}/projects.v1.json`, []),
      this.fetchWithFallback(`${primaryBase}/${files.blogs || 'blogs.v1.json'}`, `${fallbackBase}/blogs.v1.json`, [])
    ]);

    return { profile, sections, achievements, experience, skills, projects, blogs };
  },

  /**
   * Robust per-file fetcher: Primary versioned file -> Bundled fallback file -> Default value
   */
  async fetchWithFallback(primaryUrl, fallbackUrl, defaultVal = {}) {
    try {
      const res = await fetch(primaryUrl);
      if (!res.ok) throw new Error(`HTTP ${res.status} for ${primaryUrl}`);
      return await res.json();
    } catch (primaryErr) {
      console.warn(`Primary file fetch failed for ${primaryUrl}. Attempting fallback ${fallbackUrl}...`);
      try {
        const fallbackRes = await fetch(fallbackUrl);
        if (!fallbackRes.ok) throw new Error(`HTTP ${fallbackRes.status} for fallback ${fallbackUrl}`);
        return await fallbackRes.json();
      } catch (fallbackErr) {
        console.error(`Both primary and fallback failed for ${primaryUrl} -> ${fallbackUrl}:`, fallbackErr);
        return defaultVal;
      }
    }
  },

  /**
   * Populate Permanent Hero Section dynamically from published Profile JSON
   */
  updateHeroProfile(profile) {
    if (!profile) return;

    const name = profile.name ? profile.name.trim() : '';
    const title = profile.title ? profile.title.trim() : '';

    // 1. Dynamic Hero Name
    const nameElem = document.getElementById('hero-display-name');
    if (nameElem) {
      nameElem.textContent = name;
    }

    // 2. Dynamic Document Title & Meta Description
    if (name) {
      document.title = title ? `${name} — ${title}` : `${name} — Portfolio`;
    }
    const metaDesc = document.getElementById('meta-description');
    if (metaDesc && profile.bio) {
      metaDesc.setAttribute('content', profile.bio);
    }

    // 3. Dynamic Subtitle / Headline
    const subtitleElem = document.getElementById('hero-subtitle');
    if (subtitleElem) {
      subtitleElem.textContent = title;
    }

    // 4. Dynamic Bio / Statement
    const bioElem = document.getElementById('hero-bio');
    if (bioElem) {
      bioElem.textContent = profile.bio || '';
    }

    // 5. Dynamic Profile Avatar Image (Empty clean square placeholder if empty/null)
    const avatarElem = document.getElementById('hero-avatar-img');
    const avatarContainer = document.querySelector('.hero-image-container');
    if (avatarElem) {
      if (profile.avatarUrl && typeof profile.avatarUrl === 'string' && profile.avatarUrl.trim() !== '') {
        avatarElem.src = profile.avatarUrl.trim();
        avatarElem.alt = name ? `${name} Avatar` : 'Profile Avatar';
        avatarElem.style.display = 'block';
        if (avatarContainer) avatarContainer.classList.add('has-image');
      } else {
        avatarElem.src = '';
        avatarElem.alt = '';
        avatarElem.style.display = 'none';
        if (avatarContainer) avatarContainer.classList.remove('has-image');
      }
    }

    // 6. Dynamic Footer Name & Copyright Year
    const footerName = document.getElementById('footer-name');
    if (footerName && name) {
      footerName.textContent = name;
    }
    const articleFooterName = document.getElementById('article-footer-name');
    if (articleFooterName && name) {
      articleFooterName.textContent = name;
    }

    const currentYear = new Date().getFullYear();
    const footerYear = document.getElementById('footer-year');
    if (footerYear) footerYear.textContent = currentYear;
    const articleFooterYear = document.getElementById('article-footer-year');
    if (articleFooterYear) articleFooterYear.textContent = currentYear;
  },

  /**
   * Dynamically bind Hero Action buttons (View Work / Contact) to published visible section IDs
   */
  updateHeroActions(sections) {
    const workBtn = document.getElementById('hero-work-btn');
    const contactBtn = document.getElementById('hero-contact-btn');

    const visibleSections = Array.isArray(sections) ? sections.filter(s => s.visible !== false) : [];

    // Find visible PROJECTS section dynamically
    const projectsSec = visibleSections.find(s => s.type === 'PROJECTS') ||
                        visibleSections.find(s => s.id && s.id.toLowerCase().includes('project'));
    if (workBtn) {
      if (projectsSec && projectsSec.id) {
        workBtn.href = `#${projectsSec.id}`;
        workBtn.style.display = 'inline-flex';
      } else {
        workBtn.style.display = 'none';
      }
    }

    // Find visible CONTACT section dynamically
    const contactSec = visibleSections.find(s => s.type === 'CONTACT') ||
                       visibleSections.find(s => s.id && s.id.toLowerCase().includes('contact'));
    if (contactBtn) {
      if (contactSec && contactSec.id) {
        contactBtn.href = `#${contactSec.id}`;
        contactBtn.style.display = 'inline-flex';
      } else {
        contactBtn.style.display = 'none';
      }
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

    // Update Hero actions based on resolved sections
    this.updateHeroActions(sections);

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
   * Dedicated Article Page Router for /blog/<slug>
   */
  setupBlogRouter() {
    const portfolioView = document.getElementById('portfolio-view');
    const articleView = document.getElementById('article-view');
    const backBtn = document.getElementById('article-back-btn');
    const sidebarHomeLink = document.getElementById('sidebar-home-link');

    if (!portfolioView || !articleView) return;

    const getSlugFromLocation = () => {
      const path = window.location.pathname;
      if (path.startsWith('/blog/')) {
        return path.replace(/^\/blog\//, '').replace(/\/$/, '').trim();
      }
      // Check query parameter fallback: ?blog=my-slug
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('blog')) {
        return urlParams.get('blog').trim();
      }
      // Check hash route fallback: #blog/my-slug
      const hash = window.location.hash;
      if (hash.startsWith('#blog/')) {
        return hash.replace(/^#blog\//, '').trim();
      }
      return null;
    };

    const showArticlePage = (article, pushUrl = true) => {
      portfolioView.style.display = 'none';
      articleView.style.display = 'block';
      window.scrollTo(0, 0);

      const profile = (this.publishedData && this.publishedData.profile) ? this.publishedData.profile : {};
      const authorName = profile.name || '';

      if (article) {
        document.title = authorName ? `${article.title} — ${authorName}` : `${article.title} — Article`;

        const dateMeta = document.getElementById('article-date-meta');
        if (dateMeta) dateMeta.textContent = article.date || '';

        const readMeta = document.getElementById('article-read-meta');
        if (readMeta) readMeta.textContent = article.readTime ? `• ${article.readTime}` : '';

        const titleHeading = document.getElementById('article-title-heading');
        if (titleHeading) titleHeading.textContent = article.title;

        const summaryLead = document.getElementById('article-summary-lead');
        if (summaryLead) {
          summaryLead.textContent = article.summary || '';
          summaryLead.style.display = article.summary ? 'block' : 'none';
        }

        const tagsContainer = document.getElementById('article-tags-container');
        if (tagsContainer) {
          const tags = Array.isArray(article.tags) ? article.tags : [];
          tagsContainer.innerHTML = tags.map(t => `<span class="article-page-tag">${Renderer.sanitizeHTML(t)}</span>`).join('');
          tagsContainer.style.display = tags.length ? 'flex' : 'none';
        }

        const bodyContent = document.getElementById('article-body-content');
        if (bodyContent) {
          const rawContent = article.contentMarkdown || article.content || article.summary || '';
          bodyContent.innerHTML = Renderer.renderMarkdown(rawContent);
        }
      } else {
        // Not found / Draft state
        document.title = 'Article Not Found';
        const titleHeading = document.getElementById('article-title-heading');
        if (titleHeading) titleHeading.textContent = 'Article Not Found';

        const summaryLead = document.getElementById('article-summary-lead');
        if (summaryLead) {
          summaryLead.textContent = 'The requested article is unavailable, unpublished, or has been moved.';
          summaryLead.style.display = 'block';
        }

        const bodyContent = document.getElementById('article-body-content');
        if (bodyContent) bodyContent.innerHTML = '<p><a href="/" class="btn-minimal">Return to Portfolio ↗</a></p>';
      }

      if (pushUrl && article) {
        const slug = article.slug || article.id;
        const targetPath = `/blog/${slug}`;
        if (window.location.pathname !== targetPath) {
          try {
            history.pushState({ blogSlug: slug }, '', targetPath);
          } catch (e) {
            // Hash fallback if pushState path mutation is restricted
            window.location.hash = `blog/${slug}`;
          }
        }
      }
    };

    const showPortfolioPage = (pushUrl = true) => {
      articleView.style.display = 'none';
      portfolioView.style.display = 'block';

      const profile = (this.publishedData && this.publishedData.profile) ? this.publishedData.profile : {};
      const name = profile.name ? profile.name.trim() : '';
      const title = profile.title ? profile.title.trim() : '';
      if (name) {
        document.title = title ? `${name} — ${title}` : `${name} — Portfolio`;
      }

      if (pushUrl && (window.location.pathname.startsWith('/blog/') || window.location.hash.startsWith('#blog/'))) {
        try {
          history.pushState(null, '', '/');
        } catch (e) {
          window.location.hash = '';
        }
      }
    };

    const routeCurrentUrl = () => {
      const slug = getSlugFromLocation();
      if (slug) {
        const blogs = (this.publishedData && this.publishedData.blogs) ? this.publishedData.blogs : [];
        const article = blogs.find(b => String(b.slug) === slug || String(b.id) === slug);
        showArticlePage(article, false);
      } else {
        showPortfolioPage(false);
      }
    };

    // Handle clicking on blog cards in the portfolio
    document.addEventListener('click', (e) => {
      const blogCard = e.target.closest('.blog-editorial-item');
      if (blogCard) {
        e.preventDefault();
        const slug = blogCard.getAttribute('data-blog-slug');
        const blogs = (this.publishedData && this.publishedData.blogs) ? this.publishedData.blogs : [];
        const article = blogs.find(b => String(b.slug) === slug || String(b.id) === slug);
        if (article) {
          showArticlePage(article, true);
        }
      }
    });

    // Back to overview button
    if (backBtn) {
      backBtn.addEventListener('click', (e) => {
        e.preventDefault();
        showPortfolioPage(true);
      });
    }

    if (sidebarHomeLink) {
      sidebarHomeLink.addEventListener('click', (e) => {
        if (articleView.style.display === 'block') {
          e.preventDefault();
          showPortfolioPage(true);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
      });
    }

    // Handle browser back/forward buttons
    window.addEventListener('popstate', () => {
      routeCurrentUrl();
    });

    // Initial routing evaluation
    routeCurrentUrl();
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

