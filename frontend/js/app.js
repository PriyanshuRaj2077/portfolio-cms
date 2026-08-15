/* ==========================================================================
   MAIN APPLICATION LOADER & MANIFEST RESOLVER
   Zero Cold-Start Static Architecture
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

const App = {
  async init() {
    console.log("Initializing Priyanshu's Portfolio...");
    try {
      const data = await this.loadPublishedContent();
      this.updateHeroProfile(data.profile);
      this.renderDynamicSections(data);
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
   * Populate Permanent Hero Section
   */
  updateHeroProfile(profile) {
    if (!profile) return;

    const taglineElem = document.getElementById('hero-tagline');
    if (taglineElem && profile.title) {
      const parts = profile.title.split('•').map(p => Renderer.sanitizeHTML(p.trim()));
      taglineElem.innerHTML = parts.join(' <span class="bullet">•</span> ');
    }

    const avatarElem = document.getElementById('hero-avatar-img');
    if (avatarElem && profile.avatarUrl) {
      avatarElem.src = profile.avatarUrl;
      avatarElem.alt = `${profile.name || 'Priyanshu'} illustration`;
    }
  },

  /**
   * Render dynamic CMS sections below the Hero
   */
  renderDynamicSections(data) {
    const container = document.getElementById('sections-container');
    const navContainer = document.getElementById('dynamic-nav-links');

    if (!container) return;
    container.innerHTML = '';

    const sections = (data.sections || []).filter(s => s.visible).sort((a, b) => a.order - b.order);

    if (navContainer) {
      navContainer.innerHTML = '';
    }

    sections.forEach(section => {
      // Create section element
      const sectionNode = Renderer.renderSection(section, data);
      container.appendChild(sectionNode);

      // Create nav link if navContainer exists
      if (navContainer) {
        const li = document.createElement('li');
        li.innerHTML = `<a href="#${sectionNode.id}" class="nav-link">${Renderer.sanitizeHTML(section.title)}</a>`;
        navContainer.appendChild(li);
      }
    });
  },

  showGracefulErrorState() {
    const container = document.getElementById('sections-container');
    if (container) {
      container.innerHTML = `
        <div class="container" style="padding: 4rem 0; text-align: center;">
          <h2 style="color: var(--accent-orange); margin-bottom: 1rem;">Portfolio Content Temporarily Unavailable</h2>
          <p style="color: var(--text-secondary);">We are unable to load the published portfolio content right now. Please check back in a few moments.</p>
        </div>
      `;
    }
  }
};
