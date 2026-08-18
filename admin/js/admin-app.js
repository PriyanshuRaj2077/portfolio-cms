/* ==========================================================================
   ADMIN CMS APPLICATION CONTROLLER
   Unified Minimal Editorial Management Interface
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  AdminApp.init();
});

const AdminApp = {
  currentTab: 'profile',
  authenticated: false,

  async init() {
    this.setupEventListeners();
    try {
      const user = await AdminAPI.checkSession();
      if (user && user.authenticated) {
        this.onLoginSuccess(user);
      } else {
        this.showLogin();
      }
    } catch (e) {
      this.showLogin();
    }
  },

  setupEventListeners() {
    // Login form submission
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
      loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        const errDiv = document.getElementById('login-error');
        errDiv.style.display = 'none';

        try {
          const res = await AdminAPI.login(username, password);
          if (res.authenticated) {
            this.onLoginSuccess(res);
          } else {
            errDiv.textContent = res.message || 'Invalid credentials';
            errDiv.style.display = 'block';
          }
        } catch (err) {
          errDiv.textContent = err.message || 'Login failed. Verify credentials.';
          errDiv.style.display = 'block';
        }
      });
    }

    // Mobile Sidebar Drawer controls
    const mobileToggle = document.getElementById('mobile-menu-toggle');
    const sidebar = document.getElementById('admin-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    const closeBtn = document.getElementById('sidebar-close-btn');

    const toggleSidebar = () => {
      if (sidebar) {
        const isOpen = sidebar.classList.toggle('open');
        if (backdrop) backdrop.classList.toggle('active', isOpen);
      }
    };
    const closeSidebar = () => {
      if (sidebar) sidebar.classList.remove('open');
      if (backdrop) backdrop.classList.remove('active');
    };

    if (mobileToggle) mobileToggle.addEventListener('click', toggleSidebar);
    if (closeBtn) closeBtn.addEventListener('click', closeSidebar);
    if (backdrop) backdrop.addEventListener('click', closeSidebar);

    // Sidebar navigation clicks
    document.querySelectorAll('.sidebar-nav .nav-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const tab = e.currentTarget.getAttribute('data-tab');
        if (tab) {
          this.switchTab(tab);
          closeSidebar();
        }
      });
    });

    // Logout buttons
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        await AdminAPI.logout();
        closeSidebar();
        this.showLogin();
      });
    }

    // Publish buttons (topbar and sidebar)
    const publishBtn = document.getElementById('publish-site-btn');
    if (publishBtn) {
      publishBtn.addEventListener('click', () => this.handlePublish());
    }
    const sidebarPublishBtn = document.getElementById('sidebar-publish-btn');
    if (sidebarPublishBtn) {
      sidebarPublishBtn.addEventListener('click', () => {
        closeSidebar();
        this.handlePublish();
      });
    }
  },

  showLogin() {
    this.authenticated = false;
    document.getElementById('login-overlay').style.display = 'flex';
    document.getElementById('admin-layout').style.display = 'none';
  },

  onLoginSuccess(user) {
    this.authenticated = true;
    document.getElementById('login-overlay').style.display = 'none';
    document.getElementById('admin-layout').style.display = 'flex';
    this.switchTab('profile');
  },

  async switchTab(tabName) {
    this.currentTab = tabName;

    // Close mobile drawer if open
    const sidebar = document.getElementById('admin-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('active');

    // Update nav link active styles
    document.querySelectorAll('.sidebar-nav .nav-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
    });

    // Update topbar title
    const topbarTitle = document.getElementById('topbar-title');
    if (topbarTitle) {
      const titles = {
        profile: 'Profile & Identity',
        sections: 'Dynamic Sections',
        projects: 'Projects Management',
        skills: 'Tech Stack & Skills',
        experience: 'Work Experience',
        achievements: 'Achievements',
        blogs: 'Blogs & Articles',
        media: 'Media Library'
      };
      topbarTitle.textContent = titles[tabName] || (tabName.toUpperCase() + ' // MANAGEMENT');
    }

    // Render tab view
    const mainView = document.getElementById('admin-main-view');
    mainView.innerHTML = '<div style="padding: 2rem 0; color: var(--text-muted); font-family: var(--font-mono); font-size: 0.85rem;">Loading data...</div>';

    switch (tabName) {
      case 'profile': await this.renderProfileView(mainView); break;
      case 'sections': await this.renderSectionsView(mainView); break;
      case 'projects': await this.renderProjectsView(mainView); break;
      case 'skills': await this.renderSkillsView(mainView); break;
      case 'experience': await this.renderExperienceView(mainView); break;
      case 'achievements': await this.renderAchievementsView(mainView); break;
      case 'blogs': await this.renderBlogsView(mainView); break;
      case 'media': await this.renderMediaView(mainView); break;
      default: mainView.innerHTML = '<p>View not found.</p>';
    }
  },

  showBanner(message, isError = false) {
    const existing = document.querySelector('.status-banner');
    if (existing) existing.remove();

    const banner = document.createElement('div');
    banner.className = `status-banner ${isError ? 'error' : 'success'}`;
    banner.innerHTML = `
      <span>${isError ? '✕' : '✓'} ${message}</span>
      <button onclick="this.parentElement.remove()" style="background:none; border:none; color:inherit; cursor:pointer; font-size:1.1rem; margin-left:0.5rem;">✕</button>
    `;

    document.body.appendChild(banner);
    setTimeout(() => { if (banner.parentElement) banner.remove(); }, 6000);
  },

  async handlePublish() {
    const btns = [document.getElementById('publish-site-btn'), document.getElementById('sidebar-publish-btn')];
    btns.forEach(b => { if (b) { b.disabled = true; b.textContent = 'Publishing...'; } });

    try {
      const result = await AdminAPI.publish();
      this.showBanner(`Atomic Publication successful: Manifest updated to version v${result.version}`);
    } catch (err) {
      this.showBanner(`Publication Failed: ${err.message}`, true);
    } finally {
      btns.forEach(b => { if (b) { b.disabled = false; b.textContent = 'Publish Changes ↗'; } });
    }
  },

  // =========================================================================
  // 1. PROFILE MANAGEMENT VIEW
  // =========================================================================
  async renderProfileView(container) {
    let profile = {};
    try { profile = await AdminAPI.getProfile() || {}; } catch(e) { profile = {}; }

    const avatarUrl = profile.avatarUrl || '';

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Profile & Identity</h2>
            <p class="card-subtitle">Manage personal information and avatar image</p>
          </div>
          <button id="profile-save-btn" class="btn btn-primary">Save Profile ↘</button>
        </div>

        <!-- Avatar Image Upload Workflow -->
        <div class="form-group">
          <label class="form-label">Profile Avatar Image</label>
          <div class="avatar-workflow-container">
            <div class="avatar-preview-square" id="avatar-preview-box">
              ${avatarUrl ? `<img src="${avatarUrl}" alt="Avatar" id="avatar-img-elem">` : `<div class="avatar-empty-label">[ NO IMAGE ]</div>`}
            </div>
            <div style="display: flex; flex-direction: column; gap: 0.75rem; flex-grow: 1;">
              <div>
                <input type="file" id="avatar-file-input" accept="image/*" style="display: none;">
                <button type="button" id="avatar-choose-btn" class="btn btn-secondary btn-sm">Upload New Image ↗</button>
                <button type="button" id="avatar-clear-btn" class="btn btn-danger btn-sm" ${avatarUrl ? '' : 'style="display:none;"'}>Remove Avatar</button>
              </div>
              <p class="form-helper">Upload a portrait image. The image will be stored in Media and served at a public URL. If removed, the empty square placeholder will render on the public portfolio.</p>
              <div>
                <label class="form-label" style="font-size: 0.72rem;">Image URL / Path</label>
                <input type="text" id="prof-avatar" class="form-input" value="${avatarUrl}" placeholder="/media/...">
              </div>
            </div>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label" for="prof-name">Display Name *</label>
            <input type="text" id="prof-name" class="form-input" value="${profile.name || ''}" placeholder="e.g. PRIYANSHU" required>
            <p class="form-helper">Rendered directly in the public Hero header.</p>
          </div>

          <div class="form-group">
            <label class="form-label" for="prof-title">Headline / Subtitle</label>
            <input type="text" id="prof-title" class="form-input" value="${profile.title || ''}" placeholder="e.g. Software & Systems Engineering">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="prof-bio">Bio / Statement</label>
          <textarea id="prof-bio" class="form-textarea" placeholder="Personal engineering summary...">${profile.bio || ''}</textarea>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label" for="prof-location">Location</label>
            <input type="text" id="prof-location" class="form-input" value="${profile.location || ''}" placeholder="e.g. Global / Remote">
          </div>

          <div class="form-group">
            <label class="form-label" for="prof-email">Email Address</label>
            <input type="email" id="prof-email" class="form-input" value="${profile.email || ''}" placeholder="contact@example.com">
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label" for="prof-github">GitHub URL</label>
            <input type="url" id="prof-github" class="form-input" value="${profile.githubUrl || ''}" placeholder="https://github.com/username">
          </div>

          <div class="form-group">
            <label class="form-label" for="prof-linkedin">LinkedIn URL</label>
            <input type="url" id="prof-linkedin" class="form-input" value="${profile.linkedinUrl || ''}" placeholder="https://linkedin.com/in/username">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="prof-twitter">Twitter / X / Social URL</label>
          <input type="url" id="prof-twitter" class="form-input" value="${profile.twitterUrl || ''}" placeholder="https://x.com/username">
        </div>
      </div>
    `;

    // File choose button trigger
    const fileInput = document.getElementById('avatar-file-input');
    const chooseBtn = document.getElementById('avatar-choose-btn');
    const clearBtn = document.getElementById('avatar-clear-btn');
    const avatarInput = document.getElementById('prof-avatar');
    const previewBox = document.getElementById('avatar-preview-box');

    chooseBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', async () => {
      const file = fileInput.files[0];
      if (!file) return;

      chooseBtn.disabled = true;
      chooseBtn.textContent = 'Uploading...';

      try {
        const res = await AdminAPI.uploadMedia(file);
        const url = res.fileUrl || res.url;
        avatarInput.value = url;
        previewBox.innerHTML = `<img src="${url}" alt="Avatar" id="avatar-img-elem">`;
        clearBtn.style.display = 'inline-flex';
        AdminApp.showBanner('Avatar image uploaded successfully.');
      } catch (e) {
        AdminApp.showBanner('Image upload failed: ' + e.message, true);
      } finally {
        chooseBtn.disabled = false;
        chooseBtn.textContent = 'Upload New Image ↗';
      }
    });

    clearBtn.addEventListener('click', () => {
      avatarInput.value = '';
      previewBox.innerHTML = `<div class="avatar-empty-label">[ NO IMAGE ]</div>`;
      clearBtn.style.display = 'none';
    });

    avatarInput.addEventListener('input', () => {
      const val = avatarInput.value.trim();
      if (val) {
        previewBox.innerHTML = `<img src="${val}" alt="Avatar" id="avatar-img-elem">`;
        clearBtn.style.display = 'inline-flex';
      } else {
        previewBox.innerHTML = `<div class="avatar-empty-label">[ NO IMAGE ]</div>`;
        clearBtn.style.display = 'none';
      }
    });

    // Save profile handler
    document.getElementById('profile-save-btn').addEventListener('click', async () => {
      const saveBtn = document.getElementById('profile-save-btn');
      saveBtn.disabled = true;
      saveBtn.textContent = 'Saving...';

      const payload = {
        id: profile.id,
        name: document.getElementById('prof-name').value.trim(),
        title: document.getElementById('prof-title').value.trim(),
        bio: document.getElementById('prof-bio').value.trim(),
        location: document.getElementById('prof-location').value.trim(),
        email: document.getElementById('prof-email').value.trim(),
        githubUrl: document.getElementById('prof-github').value.trim(),
        linkedinUrl: document.getElementById('prof-linkedin').value.trim(),
        twitterUrl: document.getElementById('prof-twitter').value.trim(),
        avatarUrl: avatarInput.value.trim()
      };

      try {
        await AdminAPI.updateProfile(payload);
        AdminApp.showBanner('Profile updated. Click Publish to apply to the public site.');
      } catch (err) {
        AdminApp.showBanner('Failed to save profile: ' + err.message, true);
      } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = 'Save Profile ↘';
      }
    });
  },

  // =========================================================================
  // 2. SECTIONS MANAGEMENT VIEW
  // =========================================================================
  async renderSectionsView(container) {
    let sections = [];
    try { sections = await AdminAPI.getSections() || []; } catch(e) { sections = []; }

    // Deterministic sort: visible sections first by order, then hidden
    sections.sort((a, b) => {
      const aVis = a.visible !== false;
      const bVis = b.visible !== false;
      if (aVis && !bVis) return -1;
      if (!aVis && bVis) return 1;
      return (a.order || a.sortOrder || 0) - (b.order || b.sortOrder || 0);
    });

    const visibleSections = sections.filter(s => s.visible !== false);
    const visibleCount = visibleSections.length;

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Dynamic Sections</h2>
            <p class="card-subtitle">Manage portfolio sections, deterministic display order, and single-letter navigation</p>
          </div>
          <button id="new-section-btn" class="btn btn-primary">+ Add New Section</button>
        </div>

        <!-- Section Form Modal / In-Place Editor -->
        <div id="section-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="section-form-title" class="card-title" style="font-size: 1rem;">Add Section</h3>
            <button id="section-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="section-form">
            <input type="hidden" id="sec-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="sec-title">Section Title *</label>
                <input type="text" id="sec-title" class="form-input" required placeholder="e.g. Photography">
              </div>
              <div class="form-group">
                <label class="form-label" for="sec-letter">Navigation Letter * (Exactly 1 Uppercase Letter [A-Z])</label>
                <input type="text" id="sec-letter" class="form-input" maxlength="1" required placeholder="e.g. F" style="text-transform: uppercase; font-family: var(--font-mono); width: 100px;">
                <p class="form-helper">Must be a single uppercase letter. Duplicates among visible sections will be rejected.</p>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="sec-type">Section Type</label>
                <select id="sec-type" class="form-select">
                  <option value="PROJECTS">PROJECTS (Projects Grid)</option>
                  <option value="SKILLS">SKILLS (Tech Stack Grid)</option>
                  <option value="TIMELINE">TIMELINE (Experience / History)</option>
                  <option value="ACHIEVEMENTS">ACHIEVEMENTS (Highlights)</option>
                  <option value="BLOG">BLOG (Articles / Journal)</option>
                  <option value="CONTACT">CONTACT (Contact & Social Links)</option>
                  <option value="TEXT">TEXT (Custom Editorial Text)</option>
                  <option value="GALLERY">GALLERY (Media Gallery)</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label" for="sec-order">Position / Sort Order (1 to ${visibleCount + 1})</label>
                <input type="number" id="sec-order" class="form-input" value="1" min="1" max="${visibleCount + 1}">
                <p class="form-helper">Choosing an occupied position automatically shifts other visible sections.</p>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="sec-label">Section Tagline / Number Label</label>
              <input type="text" id="sec-label" class="form-input" placeholder="e.g. 07 // GALLERY">
            </div>

            <div class="form-group">
              <label class="form-label" for="sec-desc">Description / Subtext</label>
              <textarea id="sec-desc" class="form-textarea" placeholder="Optional editorial subtext..."></textarea>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" id="sec-visible" checked>
                <span>Visible on public site & navigation rail</span>
              </label>
            </div>

            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem; flex-wrap: wrap;">
              <button type="submit" class="btn btn-primary">Save Section ↘</button>
              <button type="button" id="section-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <!-- Sections List -->
        <div class="item-row-list" id="sections-list">
          ${sections.length === 0 ? '<p style="color: var(--text-muted);">No sections created yet.</p>' : ''}
          ${sections.map((sec, idx) => {
            const isVis = sec.visible !== false;
            const currentOrder = sec.order || sec.sortOrder || 1;
            const canMoveUp = isVis && currentOrder > 1;
            const canMoveDown = isVis && currentOrder < visibleCount;

            return `
            <div class="item-row" data-id="${sec.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="tag-letter">${sec.navLetter || '—'}</span>
                  <span class="item-title">${sec.title}</span>
                  <span class="tag-badge ${isVis ? 'published' : 'draft'}">${isVis ? `POS #${currentOrder}` : 'HIDDEN'}</span>
                  <span class="tag-badge">${sec.type || 'TEXT'}</span>
                </div>
                <div class="item-meta">Letter: <strong>${sec.navLetter || '—'}</strong> • Order: ${currentOrder} • ID: ${sec.id}</div>
                ${sec.description ? `<div class="item-subtext">${sec.description}</div>` : ''}
              </div>
              <div class="item-actions">
                ${isVis ? `
                  <button class="btn btn-secondary btn-sm sec-move-up-btn" data-id="${sec.id}" ${canMoveUp ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'} title="Move Up">↑ Up</button>
                  <button class="btn btn-secondary btn-sm sec-move-down-btn" data-id="${sec.id}" ${canMoveDown ? '' : 'disabled style="opacity:0.4; cursor:not-allowed;"'} title="Move Down">↓ Down</button>
                ` : ''}
                <button class="btn btn-secondary btn-sm sec-edit-btn" data-id="${sec.id}">Edit</button>
                <button class="btn btn-danger btn-sm sec-del-btn" data-id="${sec.id}">Delete</button>
              </div>
            </div>
          `;
          }).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('section-form-card');
    const formTitle = document.getElementById('section-form-title');
    const newBtn = document.getElementById('new-section-btn');
    const cancelBtn = document.getElementById('section-cancel-btn');
    const cancelBtn2 = document.getElementById('section-cancel-btn-2');

    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Section: ${data.title}`;
        document.getElementById('sec-id').value = data.id || '';
        document.getElementById('sec-title').value = data.title || '';
        document.getElementById('sec-letter').value = data.navLetter || '';
        document.getElementById('sec-type').value = data.type || 'PROJECTS';
        document.getElementById('sec-order').value = data.order || data.sortOrder || 1;
        document.getElementById('sec-label').value = data.label || '';
        document.getElementById('sec-desc').value = data.description || '';
        document.getElementById('sec-visible').checked = data.visible !== false;
      } else {
        formTitle.textContent = 'Add New Section';
        document.getElementById('sec-id').value = '';
        document.getElementById('sec-title').value = '';
        document.getElementById('sec-letter').value = '';
        document.getElementById('sec-type').value = 'TEXT';
        document.getElementById('sec-order').value = visibleCount + 1;
        document.getElementById('sec-label').value = '';
        document.getElementById('sec-desc').value = '';
        document.getElementById('sec-visible').checked = true;
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    const closeForm = () => { formCard.style.display = 'none'; };

    newBtn.addEventListener('click', () => openForm());
    cancelBtn.addEventListener('click', closeForm);
    cancelBtn2.addEventListener('click', closeForm);

    // Quick Move Up button
    container.querySelectorAll('.sec-move-up-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const sec = sections.find(s => s.id === id);
        if (sec && (sec.order || sec.sortOrder || 1) > 1) {
          try {
            btn.disabled = true;
            await AdminAPI.saveSection({ ...sec, order: (sec.order || sec.sortOrder || 1) - 1 });
            AdminApp.showBanner(`Section "${sec.title}" moved up.`);
            AdminApp.switchTab('sections');
          } catch (err) {
            AdminApp.showBanner('Reorder failed: ' + err.message, true);
          }
        }
      });
    });

    // Quick Move Down button
    container.querySelectorAll('.sec-move-down-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const sec = sections.find(s => s.id === id);
        if (sec && (sec.order || sec.sortOrder || 1) < visibleCount) {
          try {
            btn.disabled = true;
            await AdminAPI.saveSection({ ...sec, order: (sec.order || sec.sortOrder || 1) + 1 });
            AdminApp.showBanner(`Section "${sec.title}" moved down.`);
            AdminApp.switchTab('sections');
          } catch (err) {
            AdminApp.showBanner('Reorder failed: ' + err.message, true);
          }
        }
      });
    });

    // Edit button click
    container.querySelectorAll('.sec-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const sec = sections.find(s => s.id === id);
        if (sec) openForm(sec);
      });
    });

    // Delete button click
    container.querySelectorAll('.sec-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm(`Are you sure you want to delete section "${id}"?`)) {
          try {
            await AdminAPI.deleteSection(id);
            AdminApp.showBanner('Section deleted.');
            AdminApp.switchTab('sections');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    // Form submit with Single-Letter Navigation Validation & Auto Reordering
    document.getElementById('section-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('sec-id').value.trim();
      const title = document.getElementById('sec-title').value.trim();
      let rawLetter = document.getElementById('sec-letter').value.trim().toUpperCase();
      const type = document.getElementById('sec-type').value;
      const order = parseInt(document.getElementById('sec-order').value, 10) || 1;
      const label = document.getElementById('sec-label').value.trim();
      const description = document.getElementById('sec-desc').value.trim();
      const visible = document.getElementById('sec-visible').checked;

      // Validation 1: Exactly one uppercase character [A-Z]
      if (!/^[A-Z]$/.test(rawLetter)) {
        AdminApp.showBanner('Invalid navigation letter: Must be exactly one uppercase letter [A-Z].', true);
        return;
      }

      // Validation 2: Duplicate check among visible sections
      if (visible) {
        const duplicate = sections.find(s => s.id !== id && s.visible !== false && (s.navLetter || '').toUpperCase() === rawLetter);
        if (duplicate) {
          AdminApp.showBanner(`Navigation letter "${rawLetter}" is already assigned to another visible section (${duplicate.title}).`, true);
          return;
        }
      }

      const payload = {
        id: id || undefined,
        title,
        navLetter: rawLetter,
        icon: rawLetter,
        type,
        order,
        label,
        description,
        visible,
        theme: 'default'
      };

      try {
        await AdminAPI.saveSection(payload);
        AdminApp.showBanner('Section saved and reordered successfully.');
        AdminApp.switchTab('sections');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 3. PROJECTS MANAGEMENT VIEW
  // =========================================================================
  async renderProjectsView(container) {
    let projects = [];
    try { projects = await AdminAPI.getProjects() || []; } catch(e) { projects = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Projects</h2>
            <p class="card-subtitle">Manage portfolio work, case studies, links, and tags</p>
          </div>
          <button id="new-project-btn" class="btn btn-primary">+ Add New Project</button>
        </div>

        <!-- Project Form -->
        <div id="project-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="project-form-title" class="card-title" style="font-size: 1rem;">Add Project</h3>
            <button id="project-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="project-form">
            <input type="hidden" id="proj-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="proj-title">Project Title *</label>
                <input type="text" id="proj-title" class="form-input" required placeholder="e.g. Astra Engine">
              </div>
              <div class="form-group">
                <label class="form-label" for="proj-order">Sort Order</label>
                <input type="number" id="proj-order" class="form-input" value="1" min="1">
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="proj-summary">Short Description / Summary *</label>
              <input type="text" id="proj-summary" class="form-input" required placeholder="High-performance atomic static CMS architecture...">
            </div>

            <div class="form-group">
              <label class="form-label" for="proj-tags">Technologies / Tags (comma-separated)</label>
              <input type="text" id="proj-tags" class="form-input" placeholder="Java 21, Spring Boot, PostgreSQL, Docker">
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="proj-repo">GitHub Repository URL</label>
                <input type="url" id="proj-repo" class="form-input" placeholder="https://github.com/username/repo">
              </div>
              <div class="form-group">
                <label class="form-label" for="proj-live">Live Demo / Site URL</label>
                <input type="url" id="proj-live" class="form-input" placeholder="https://example.com">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="proj-cover">Cover Image URL</label>
                <input type="text" id="proj-cover" class="form-input" placeholder="/media/cover.png or https://...">
              </div>
              <div class="form-group">
                <label class="form-label" for="proj-status">Status</label>
                <select id="proj-status" class="form-select">
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="ARCHIVED">ARCHIVED</option>
                  <option value="WIP">WORK IN PROGRESS</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="proj-desc">Detailed Description (Optional)</label>
              <textarea id="proj-desc" class="form-textarea" placeholder="Extended architecture details..."></textarea>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" id="proj-featured" checked>
                <span>Featured Project</span>
              </label>
            </div>

            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem;">
              <button type="submit" class="btn btn-primary">Save Project ↘</button>
              <button type="button" id="project-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <!-- Projects List -->
        <div class="item-row-list">
          ${projects.length === 0 ? '<p style="color: var(--text-muted);">No projects added yet.</p>' : ''}
          ${projects.map(p => `
            <div class="item-row" data-id="${p.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="item-title">${p.title}</span>
                  ${p.featured ? '<span class="tag-badge published">FEATURED</span>' : ''}
                  <span class="tag-badge">${p.status || 'ACTIVE'}</span>
                </div>
                <div class="item-subtext">${p.summary || p.description || ''}</div>
                <div class="item-meta">Tags: ${p.tagsJson || 'None'} • Order: ${p.sortOrder || 1}</div>
              </div>
              <div class="item-actions">
                <button class="btn btn-secondary btn-sm proj-edit-btn" data-id="${p.id}">Edit</button>
                <button class="btn btn-danger btn-sm proj-del-btn" data-id="${p.id}">Delete</button>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('project-form-card');
    const formTitle = document.getElementById('project-form-title');
    const newBtn = document.getElementById('new-project-btn');
    const cancelBtn = document.getElementById('project-cancel-btn');
    const cancelBtn2 = document.getElementById('project-cancel-btn-2');

    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Project: ${data.title}`;
        document.getElementById('proj-id').value = data.id || '';
        document.getElementById('proj-title').value = data.title || '';
        document.getElementById('proj-summary').value = data.summary || '';
        document.getElementById('proj-desc').value = data.description || '';
        document.getElementById('proj-tags').value = data.tagsJson || '';
        document.getElementById('proj-repo').value = data.repoUrl || '';
        document.getElementById('proj-live').value = data.liveUrl || '';
        document.getElementById('proj-cover').value = data.coverImage || '';
        document.getElementById('proj-status').value = data.status || 'ACTIVE';
        document.getElementById('proj-order').value = data.sortOrder || 1;
        document.getElementById('proj-featured').checked = data.featured !== false;
      } else {
        formTitle.textContent = 'Add New Project';
        document.getElementById('proj-id').value = '';
        document.getElementById('proj-title').value = '';
        document.getElementById('proj-summary').value = '';
        document.getElementById('proj-desc').value = '';
        document.getElementById('proj-tags').value = '';
        document.getElementById('proj-repo').value = '';
        document.getElementById('proj-live').value = '';
        document.getElementById('proj-cover').value = '';
        document.getElementById('proj-status').value = 'ACTIVE';
        document.getElementById('proj-order').value = projects.length + 1;
        document.getElementById('proj-featured').checked = true;
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    newBtn.addEventListener('click', () => openForm());
    cancelBtn.addEventListener('click', () => formCard.style.display = 'none');
    cancelBtn2.addEventListener('click', () => formCard.style.display = 'none');

    container.querySelectorAll('.proj-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const proj = projects.find(p => p.id === id);
        if (proj) openForm(proj);
      });
    });

    container.querySelectorAll('.proj-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm(`Delete project "${id}"?`)) {
          try {
            await AdminAPI.deleteProject(id);
            AdminApp.showBanner('Project deleted.');
            AdminApp.switchTab('projects');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    document.getElementById('project-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        id: document.getElementById('proj-id').value.trim() || undefined,
        title: document.getElementById('proj-title').value.trim(),
        summary: document.getElementById('proj-summary').value.trim(),
        description: document.getElementById('proj-desc').value.trim(),
        tagsJson: document.getElementById('proj-tags').value.trim(),
        repoUrl: document.getElementById('proj-repo').value.trim(),
        liveUrl: document.getElementById('proj-live').value.trim(),
        coverImage: document.getElementById('proj-cover').value.trim(),
        status: document.getElementById('proj-status').value,
        sortOrder: parseInt(document.getElementById('proj-order').value, 10) || 1,
        featured: document.getElementById('proj-featured').checked
      };

      try {
        await AdminAPI.saveProject(payload);
        AdminApp.showBanner('Project saved successfully.');
        AdminApp.switchTab('projects');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 4. TECH STACK CMS
  // =========================================================================
  async renderSkillsView(container) {
    let skills = [];
    try { skills = await AdminAPI.getSkills() || []; } catch(e) { skills = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Tech Stack & Skills</h2>
            <p class="card-subtitle">Manage technologies and skill categories</p>
          </div>
          <button id="new-skill-btn" class="btn btn-primary">+ Add Skill</button>
        </div>

        <div id="skill-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="skill-form-title" class="card-title" style="font-size: 1rem;">Add Skill</h3>
            <button id="skill-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="skill-form">
            <input type="hidden" id="skill-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="skill-name">Skill / Technology Name *</label>
                <input type="text" id="skill-name" class="form-input" required placeholder="e.g. Java / Spring Boot">
              </div>
              <div class="form-group">
                <label class="form-label" for="skill-cat">Category *</label>
                <input type="text" id="skill-cat" class="form-input" required placeholder="e.g. Backend & Systems, Frontend, Infrastructure">
              </div>
            </div>
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="skill-level">Level / Tag (Optional)</label>
                <input type="text" id="skill-level" class="form-input" placeholder="Core, Advanced, etc.">
              </div>
              <div class="form-group">
                <label class="form-label" for="skill-order">Sort Order</label>
                <input type="number" id="skill-order" class="form-input" value="1" min="1">
              </div>
            </div>
            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem;">
              <button type="submit" class="btn btn-primary">Save Skill ↘</button>
              <button type="button" id="skill-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <div class="item-row-list">
          ${skills.length === 0 ? '<p style="color: var(--text-muted);">No skills added yet.</p>' : ''}
          ${skills.map(s => `
            <div class="item-row" data-id="${s.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="item-title">${s.name}</span>
                  <span class="tag-badge">${s.category || 'General'}</span>
                  ${s.level ? `<span class="tag-badge">${s.level}</span>` : ''}
                </div>
                <div class="item-meta">Order: ${s.sortOrder || 1}</div>
              </div>
              <div class="item-actions">
                <button class="btn btn-secondary btn-sm skill-edit-btn" data-id="${s.id}">Edit</button>
                <button class="btn btn-danger btn-sm skill-del-btn" data-id="${s.id}">Delete</button>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('skill-form-card');
    const formTitle = document.getElementById('skill-form-title');
    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Skill: ${data.name}`;
        document.getElementById('skill-id').value = data.id || '';
        document.getElementById('skill-name').value = data.name || '';
        document.getElementById('skill-cat').value = data.category || '';
        document.getElementById('skill-level').value = data.level || '';
        document.getElementById('skill-order').value = data.sortOrder || 1;
      } else {
        formTitle.textContent = 'Add New Skill';
        document.getElementById('skill-id').value = '';
        document.getElementById('skill-name').value = '';
        document.getElementById('skill-cat').value = '';
        document.getElementById('skill-level').value = '';
        document.getElementById('skill-order').value = skills.length + 1;
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    document.getElementById('new-skill-btn').addEventListener('click', () => openForm());
    document.getElementById('skill-cancel-btn').addEventListener('click', () => formCard.style.display = 'none');
    document.getElementById('skill-cancel-btn-2').addEventListener('click', () => formCard.style.display = 'none');

    container.querySelectorAll('.skill-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = parseInt(e.currentTarget.getAttribute('data-id'), 10);
        const skill = skills.find(s => s.id === id);
        if (skill) openForm(skill);
      });
    });

    container.querySelectorAll('.skill-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = parseInt(e.currentTarget.getAttribute('data-id'), 10);
        if (confirm(`Delete skill?`)) {
          try {
            await AdminAPI.deleteSkill(id);
            AdminApp.showBanner('Skill deleted.');
            AdminApp.switchTab('skills');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    document.getElementById('skill-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const idVal = document.getElementById('skill-id').value.trim();
      const payload = {
        id: idVal ? parseInt(idVal, 10) : undefined,
        name: document.getElementById('skill-name').value.trim(),
        category: document.getElementById('skill-cat').value.trim(),
        level: document.getElementById('skill-level').value.trim(),
        sortOrder: parseInt(document.getElementById('skill-order').value, 10) || 1
      };

      try {
        await AdminAPI.saveSkill(payload);
        AdminApp.showBanner('Skill saved.');
        AdminApp.switchTab('skills');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 5. WORK EXPERIENCE CMS
  // =========================================================================
  async renderExperienceView(container) {
    let experience = [];
    try { experience = await AdminAPI.getExperience() || []; } catch(e) { experience = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Work Experience</h2>
            <p class="card-subtitle">Manage engineering timeline, roles, and bullet highlights</p>
          </div>
          <button id="new-exp-btn" class="btn btn-primary">+ Add Experience</button>
        </div>

        <div id="exp-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="exp-form-title" class="card-title" style="font-size: 1rem;">Add Experience</h3>
            <button id="exp-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="exp-form">
            <input type="hidden" id="exp-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="exp-company">Company / Organization *</label>
                <input type="text" id="exp-company" class="form-input" required placeholder="e.g. Acme Systems">
              </div>
              <div class="form-group">
                <label class="form-label" for="exp-role">Role / Position *</label>
                <input type="text" id="exp-role" class="form-input" required placeholder="e.g. Systems Engineer">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="exp-location">Location</label>
                <input type="text" id="exp-location" class="form-input" placeholder="e.g. Remote / San Francisco">
              </div>
              <div class="form-group">
                <label class="form-label" for="exp-order">Sort Order</label>
                <input type="number" id="exp-order" class="form-input" value="1" min="1">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="exp-start">Start Date</label>
                <input type="text" id="exp-start" class="form-input" placeholder="e.g. 2024">
              </div>
              <div class="form-group">
                <label class="form-label" for="exp-end">End Date</label>
                <input type="text" id="exp-end" class="form-input" placeholder="e.g. Present or 2026">
              </div>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" id="exp-current">
                <span>Current Role</span>
              </label>
            </div>

            <div class="form-group">
              <label class="form-label" for="exp-desc">Summary Description</label>
              <textarea id="exp-desc" class="form-textarea" placeholder="Overview of team and responsibilities..."></textarea>
            </div>

            <div class="form-group">
              <label class="form-label" for="exp-highlights">Key Highlights (One bullet per line)</label>
              <textarea id="exp-highlights" class="form-textarea" placeholder="Designed and built zero-downtime distributed service&#10;Reduced latency by 45% using async pipeline"></textarea>
            </div>

            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem;">
              <button type="submit" class="btn btn-primary">Save Experience ↘</button>
              <button type="button" id="exp-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <div class="item-row-list">
          ${experience.length === 0 ? '<p style="color: var(--text-muted);">No experience added yet.</p>' : ''}
          ${experience.map(e => `
            <div class="item-row" data-id="${e.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="item-title">${e.role}</span>
                  <span class="item-meta">— ${e.company}</span>
                  ${e.currentRole ? '<span class="tag-badge published">CURRENT</span>' : ''}
                </div>
                <div class="item-meta">${e.startDate || ''} — ${e.endDate || 'Present'} ${e.location ? `• ${e.location}` : ''}</div>
                ${e.description ? `<div class="item-subtext">${e.description}</div>` : ''}
              </div>
              <div class="item-actions">
                <button class="btn btn-secondary btn-sm exp-edit-btn" data-id="${e.id}">Edit</button>
                <button class="btn btn-danger btn-sm exp-del-btn" data-id="${e.id}">Delete</button>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('exp-form-card');
    const formTitle = document.getElementById('exp-form-title');
    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Experience: ${data.role} @ ${data.company}`;
        document.getElementById('exp-id').value = data.id || '';
        document.getElementById('exp-company').value = data.company || '';
        document.getElementById('exp-role').value = data.role || '';
        document.getElementById('exp-location').value = data.location || '';
        document.getElementById('exp-start').value = data.startDate || '';
        document.getElementById('exp-end').value = data.endDate || '';
        document.getElementById('exp-current').checked = Boolean(data.currentRole);
        document.getElementById('exp-order').value = data.sortOrder || 1;
        document.getElementById('exp-desc').value = data.description || '';

        let bullets = '';
        if (typeof data.highlightsJson === 'string') {
          try {
            const arr = JSON.parse(data.highlightsJson);
            if (Array.isArray(arr)) bullets = arr.join('\n');
          } catch(e) { bullets = data.highlightsJson; }
        }
        document.getElementById('exp-highlights').value = bullets;
      } else {
        formTitle.textContent = 'Add New Experience';
        document.getElementById('exp-id').value = '';
        document.getElementById('exp-company').value = '';
        document.getElementById('exp-role').value = '';
        document.getElementById('exp-location').value = '';
        document.getElementById('exp-start').value = '';
        document.getElementById('exp-end').value = '';
        document.getElementById('exp-current').checked = false;
        document.getElementById('exp-order').value = experience.length + 1;
        document.getElementById('exp-desc').value = '';
        document.getElementById('exp-highlights').value = '';
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    document.getElementById('new-exp-btn').addEventListener('click', () => openForm());
    document.getElementById('exp-cancel-btn').addEventListener('click', () => formCard.style.display = 'none');
    document.getElementById('exp-cancel-btn-2').addEventListener('click', () => formCard.style.display = 'none');

    container.querySelectorAll('.exp-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const exp = experience.find(x => x.id === id);
        if (exp) openForm(exp);
      });
    });

    container.querySelectorAll('.exp-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm(`Delete experience record?`)) {
          try {
            await AdminAPI.deleteExperience(id);
            AdminApp.showBanner('Experience deleted.');
            AdminApp.switchTab('experience');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    document.getElementById('exp-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const rawHighlights = document.getElementById('exp-highlights').value
        .split('\n')
        .map(h => h.trim())
        .filter(h => h.length > 0);

      const payload = {
        id: document.getElementById('exp-id').value.trim() || undefined,
        company: document.getElementById('exp-company').value.trim(),
        role: document.getElementById('exp-role').value.trim(),
        location: document.getElementById('exp-location').value.trim(),
        startDate: document.getElementById('exp-start').value.trim(),
        endDate: document.getElementById('exp-end').value.trim(),
        currentRole: document.getElementById('exp-current').checked,
        sortOrder: parseInt(document.getElementById('exp-order').value, 10) || 1,
        description: document.getElementById('exp-desc').value.trim(),
        highlightsJson: JSON.stringify(rawHighlights)
      };

      try {
        await AdminAPI.saveExperience(payload);
        AdminApp.showBanner('Experience saved.');
        AdminApp.switchTab('experience');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 6. ACHIEVEMENTS CMS
  // =========================================================================
  async renderAchievementsView(container) {
    let achievements = [];
    try { achievements = await AdminAPI.getAchievements() || []; } catch(e) { achievements = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Achievements</h2>
            <p class="card-subtitle">Manage notable milestones, awards, and metrics</p>
          </div>
          <button id="new-ach-btn" class="btn btn-primary">+ Add Achievement</button>
        </div>

        <div id="ach-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="ach-form-title" class="card-title" style="font-size: 1rem;">Add Achievement</h3>
            <button id="ach-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="ach-form">
            <input type="hidden" id="ach-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="ach-title">Title *</label>
                <input type="text" id="ach-title" class="form-input" required placeholder="e.g. Distributed Systems Fellowship">
              </div>
              <div class="form-group">
                <label class="form-label" for="ach-metric">Metric / Highlight Badge</label>
                <input type="text" id="ach-metric" class="form-input" placeholder="e.g. Top 1% / 10M+ req/s">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="ach-issuer">Issuer / Organization</label>
                <input type="text" id="ach-issuer" class="form-input" placeholder="e.g. ACM / Google">
              </div>
              <div class="form-group">
                <label class="form-label" for="ach-date">Date / Year</label>
                <input type="text" id="ach-date" class="form-input" placeholder="e.g. 2025">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="ach-link">Reference Link (URL)</label>
                <input type="url" id="ach-link" class="form-input" placeholder="https://...">
              </div>
              <div class="form-group">
                <label class="form-label" for="ach-order">Sort Order</label>
                <input type="number" id="ach-order" class="form-input" value="1" min="1">
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="ach-desc">Description</label>
              <textarea id="ach-desc" class="form-textarea" placeholder="Details regarding this achievement..."></textarea>
            </div>

            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem;">
              <button type="submit" class="btn btn-primary">Save Achievement ↘</button>
              <button type="button" id="ach-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <div class="item-row-list">
          ${achievements.length === 0 ? '<p style="color: var(--text-muted);">No achievements added yet.</p>' : ''}
          ${achievements.map(a => `
            <div class="item-row" data-id="${a.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="item-title">${a.title}</span>
                  ${a.metric ? `<span class="tag-badge published">${a.metric}</span>` : ''}
                  ${a.issuer ? `<span class="tag-badge">${a.issuer}</span>` : ''}
                </div>
                <div class="item-meta">${a.date || ''} • Order: ${a.sortOrder || 1}</div>
                ${a.descText ? `<div class="item-subtext">${a.descText}</div>` : ''}
              </div>
              <div class="item-actions">
                <button class="btn btn-secondary btn-sm ach-edit-btn" data-id="${a.id}">Edit</button>
                <button class="btn btn-danger btn-sm ach-del-btn" data-id="${a.id}">Delete</button>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('ach-form-card');
    const formTitle = document.getElementById('ach-form-title');
    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Achievement: ${data.title}`;
        document.getElementById('ach-id').value = data.id || '';
        document.getElementById('ach-title').value = data.title || '';
        document.getElementById('ach-metric').value = data.metric || '';
        document.getElementById('ach-issuer').value = data.issuer || '';
        document.getElementById('ach-date').value = data.date || '';
        document.getElementById('ach-link').value = data.link || '';
        document.getElementById('ach-order').value = data.sortOrder || 1;
        document.getElementById('ach-desc').value = data.descText || '';
      } else {
        formTitle.textContent = 'Add New Achievement';
        document.getElementById('ach-id').value = '';
        document.getElementById('ach-title').value = '';
        document.getElementById('ach-metric').value = '';
        document.getElementById('ach-issuer').value = '';
        document.getElementById('ach-date').value = '';
        document.getElementById('ach-link').value = '';
        document.getElementById('ach-order').value = achievements.length + 1;
        document.getElementById('ach-desc').value = '';
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    document.getElementById('new-ach-btn').addEventListener('click', () => openForm());
    document.getElementById('ach-cancel-btn').addEventListener('click', () => formCard.style.display = 'none');
    document.getElementById('ach-cancel-btn-2').addEventListener('click', () => formCard.style.display = 'none');

    container.querySelectorAll('.ach-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const ach = achievements.find(a => a.id === id);
        if (ach) openForm(ach);
      });
    });

    container.querySelectorAll('.ach-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm(`Delete achievement?`)) {
          try {
            await AdminAPI.deleteAchievement(id);
            AdminApp.showBanner('Achievement deleted.');
            AdminApp.switchTab('achievements');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    document.getElementById('ach-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        id: document.getElementById('ach-id').value.trim() || undefined,
        title: document.getElementById('ach-title').value.trim(),
        metric: document.getElementById('ach-metric').value.trim(),
        issuer: document.getElementById('ach-issuer').value.trim(),
        date: document.getElementById('ach-date').value.trim(),
        link: document.getElementById('ach-link').value.trim(),
        descText: document.getElementById('ach-desc').value.trim(),
        sortOrder: parseInt(document.getElementById('ach-order').value, 10) || 1
      };

      try {
        await AdminAPI.saveAchievement(payload);
        AdminApp.showBanner('Achievement saved.');
        AdminApp.switchTab('achievements');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 7. BLOGS / ARTICLES CMS
  // =========================================================================
  async renderBlogsView(container) {
    let blogs = [];
    try { blogs = await AdminAPI.getBlogs() || []; } catch(e) { blogs = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Blogs & Writing</h2>
            <p class="card-subtitle">Manage technical essays, dedicated article URLs, and Draft/Published states</p>
          </div>
          <button id="new-blog-btn" class="btn btn-primary">+ Write New Article</button>
        </div>

        <div id="blog-form-card" class="editorial-card" style="display: none; background: var(--bg-card); border-color: var(--border-medium);">
          <div class="card-header">
            <h3 id="blog-form-title" class="card-title" style="font-size: 1rem;">New Article</h3>
            <button id="blog-cancel-btn" class="btn btn-secondary btn-sm">Cancel</button>
          </div>
          <form id="blog-form">
            <input type="hidden" id="blog-id">
            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="blog-title">Article Title *</label>
                <input type="text" id="blog-title" class="form-input" required placeholder="e.g. Architecting Zero-Cold-Start Systems">
              </div>
              <div class="form-group">
                <label class="form-label" for="blog-slug">URL Slug * (accessible via /blog/&lt;slug&gt;)</label>
                <input type="text" id="blog-slug" class="form-input" required placeholder="e.g. zero-cold-start-systems" style="font-family: var(--font-mono);">
                <p class="form-helper">Dedicated route: /blog/&lt;slug&gt;</p>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="blog-date">Publication Date</label>
                <input type="text" id="blog-date" class="form-input" placeholder="e.g. August 2026">
              </div>
              <div class="form-group">
                <label class="form-label" for="blog-readtime">Estimated Read Time</label>
                <input type="text" id="blog-readtime" class="form-input" placeholder="e.g. 5 min read">
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label" for="blog-status">Publication Status *</label>
                <select id="blog-status" class="form-select">
                  <option value="DRAFT">DRAFT (Hidden from public site)</option>
                  <option value="PUBLISHED">PUBLISHED (Visible publicly upon publish)</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label" for="blog-tags">Tags (comma-separated)</label>
                <input type="text" id="blog-tags" class="form-input" placeholder="Systems, Architecture, Java">
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="blog-summary">Abstract / Summary</label>
              <textarea id="blog-summary" class="form-textarea" placeholder="Short editorial teaser..."></textarea>
            </div>

            <div class="form-group">
              <label class="form-label" for="blog-content">Markdown Article Body</label>
              <textarea id="blog-content" class="form-textarea code-area" style="min-height: 220px;" placeholder="# Heading&#10;&#10;Technical article content in Markdown format..."></textarea>
            </div>

            <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem;">
              <button type="submit" class="btn btn-primary">Save Article ↘</button>
              <button type="button" id="blog-cancel-btn-2" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <div class="item-row-list">
          ${blogs.length === 0 ? '<p style="color: var(--text-muted);">No blog posts created yet.</p>' : ''}
          ${blogs.map(b => `
            <div class="item-row" data-id="${b.id}">
              <div class="item-main">
                <div class="item-title-line">
                  <span class="item-title">${b.title}</span>
                  <span class="tag-badge ${b.status === 'PUBLISHED' ? 'published' : 'draft'}">${b.status || 'DRAFT'}</span>
                  <span class="item-meta">/blog/${b.slug || b.id}</span>
                </div>
                <div class="item-subtext">${b.summary || ''}</div>
                <div class="item-meta">${b.date || ''} ${b.readTime ? `• ${b.readTime}` : ''}</div>
              </div>
              <div class="item-actions">
                <button class="btn btn-secondary btn-sm blog-edit-btn" data-id="${b.id}">Edit</button>
                <button class="btn btn-danger btn-sm blog-del-btn" data-id="${b.id}">Delete</button>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    const formCard = document.getElementById('blog-form-card');
    const formTitle = document.getElementById('blog-form-title');
    const openForm = (data = null) => {
      formCard.style.display = 'block';
      if (data) {
        formTitle.textContent = `Edit Article: ${data.title}`;
        document.getElementById('blog-id').value = data.id || '';
        document.getElementById('blog-title').value = data.title || '';
        document.getElementById('blog-slug').value = data.slug || '';
        document.getElementById('blog-date').value = data.date || '';
        document.getElementById('blog-readtime').value = data.readTime || '';
        document.getElementById('blog-status').value = data.status || 'DRAFT';
        document.getElementById('blog-tags').value = data.tagsJson || '';
        document.getElementById('blog-summary').value = data.summary || '';
        document.getElementById('blog-content').value = data.contentMarkdown || '';
      } else {
        formTitle.textContent = 'Write New Article';
        document.getElementById('blog-id').value = '';
        document.getElementById('blog-title').value = '';
        document.getElementById('blog-slug').value = '';
        document.getElementById('blog-date').value = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(new Date());
        document.getElementById('blog-readtime').value = '4 min read';
        document.getElementById('blog-status').value = 'DRAFT';
        document.getElementById('blog-tags').value = '';
        document.getElementById('blog-summary').value = '';
        document.getElementById('blog-content').value = '';
      }
      formCard.scrollIntoView({ behavior: 'smooth' });
    };

    // Auto-generate slug when title changes
    document.getElementById('blog-title').addEventListener('input', (e) => {
      const slugInput = document.getElementById('blog-slug');
      if (!document.getElementById('blog-id').value) {
        slugInput.value = e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
      }
    });

    document.getElementById('new-blog-btn').addEventListener('click', () => openForm());
    document.getElementById('blog-cancel-btn').addEventListener('click', () => formCard.style.display = 'none');
    document.getElementById('blog-cancel-btn-2').addEventListener('click', () => formCard.style.display = 'none');

    container.querySelectorAll('.blog-edit-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const blog = blogs.find(b => b.id === id);
        if (blog) openForm(blog);
      });
    });

    container.querySelectorAll('.blog-del-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm(`Delete article?`)) {
          try {
            await AdminAPI.deleteBlog(id);
            AdminApp.showBanner('Article deleted.');
            AdminApp.switchTab('blogs');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });

    document.getElementById('blog-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        id: document.getElementById('blog-id').value.trim() || undefined,
        title: document.getElementById('blog-title').value.trim(),
        slug: document.getElementById('blog-slug').value.trim().toLowerCase(),
        date: document.getElementById('blog-date').value.trim(),
        readTime: document.getElementById('blog-readtime').value.trim(),
        status: document.getElementById('blog-status').value,
        tagsJson: document.getElementById('blog-tags').value.trim(),
        summary: document.getElementById('blog-summary').value.trim(),
        contentMarkdown: document.getElementById('blog-content').value
      };

      try {
        await AdminAPI.saveBlog(payload);
        AdminApp.showBanner('Article saved successfully.');
        AdminApp.switchTab('blogs');
      } catch (err) {
        AdminApp.showBanner('Save failed: ' + err.message, true);
      }
    });
  },

  // =========================================================================
  // 8. MEDIA LIBRARY VIEW
  // =========================================================================
  async renderMediaView(container) {
    let mediaList = [];
    try { mediaList = await AdminAPI.getMedia() || []; } catch(e) { mediaList = []; }

    container.innerHTML = `
      <div class="editorial-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Media Library</h2>
            <p class="card-subtitle">Upload and manage image assets served via /media/...</p>
          </div>
          <div>
            <input type="file" id="media-upload-input" accept="image/*" style="display: none;">
            <button id="media-upload-btn" class="btn btn-primary">+ Upload New Image</button>
          </div>
        </div>

        <div id="media-empty-msg" style="${mediaList.length > 0 ? 'display:none;' : ''} color: var(--text-muted); font-size: 0.9rem;">
          No media files uploaded yet. Click "+ Upload New Image" to add profile avatars, project covers, or diagrams.
        </div>

        <div class="media-grid" id="media-cards-grid">
          ${mediaList.map(m => {
            const sizeKb = m.fileSize ? Math.round(m.fileSize / 1024) + ' KB' : '';
            return `
              <div class="media-card" data-id="${m.id}">
                <div class="media-thumb-box">
                  <img src="${m.fileUrl}" alt="${m.fileName}" loading="lazy">
                </div>
                <div class="media-card-body">
                  <div class="media-filename" title="${m.fileName}">${m.fileName}</div>
                  <div class="media-meta">${sizeKb} ${m.mimeType || ''}</div>
                  <div class="media-card-actions">
                    <button class="btn btn-secondary btn-sm copy-url-btn" data-url="${m.fileUrl}">Copy URL</button>
                    <button class="btn btn-secondary btn-sm set-avatar-btn" data-url="${m.fileUrl}">Set Avatar</button>
                    <button class="btn btn-danger btn-sm del-media-btn" data-id="${m.id}">Delete</button>
                  </div>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;

    const uploadInput = document.getElementById('media-upload-input');
    const uploadBtn = document.getElementById('media-upload-btn');

    uploadBtn.addEventListener('click', () => uploadInput.click());

    uploadInput.addEventListener('change', async () => {
      const file = uploadInput.files[0];
      if (!file) return;

      uploadBtn.disabled = true;
      uploadBtn.textContent = 'Uploading...';

      try {
        await AdminAPI.uploadMedia(file);
        AdminApp.showBanner('Image uploaded to media library.');
        AdminApp.switchTab('media');
      } catch (err) {
        AdminApp.showBanner('Upload failed: ' + err.message, true);
      } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = '+ Upload New Image';
      }
    });

    container.querySelectorAll('.copy-url-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const url = e.currentTarget.getAttribute('data-url');
        navigator.clipboard.writeText(url).then(() => {
          btn.textContent = 'Copied!';
          setTimeout(() => { btn.textContent = 'Copy URL'; }, 2000);
        });
      });
    });

    container.querySelectorAll('.set-avatar-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const url = e.currentTarget.getAttribute('data-url');
        try {
          const profile = await AdminAPI.getProfile() || {};
          profile.avatarUrl = url;
          await AdminAPI.updateProfile(profile);
          AdminApp.showBanner('Avatar updated in profile! Click Publish to apply to the public site.');
        } catch (err) {
          AdminApp.showBanner('Failed to update avatar: ' + err.message, true);
        }
      });
    });

    container.querySelectorAll('.del-media-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        if (confirm('Delete this media asset?')) {
          try {
            await AdminAPI.deleteMedia(id);
            AdminApp.showBanner('Media file deleted.');
            AdminApp.switchTab('media');
          } catch (err) {
            AdminApp.showBanner('Delete failed: ' + err.message, true);
          }
        }
      });
    });
  }
};
