/* ==========================================================================
   ADMIN APP CONTROLLER & SPA ROUTER
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  AdminApp.init();
});

const AdminApp = {
  currentTab: 'dashboard',
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
          errDiv.textContent = 'Login failed. Verify credentials and backend status.';
          errDiv.style.display = 'block';
        }
      });
    }

    // Sidebar navigation clicks
    document.querySelectorAll('.sidebar-nav button').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const tab = e.currentTarget.getAttribute('data-tab');
        if (tab) this.switchTab(tab);
      });
    });

    // Logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        await AdminAPI.logout();
        this.showLogin();
      });
    }

    // PUBLISH CTA Button
    const publishBtn = document.getElementById('publish-site-btn');
    if (publishBtn) {
      publishBtn.addEventListener('click', () => this.handlePublish());
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
    this.switchTab('dashboard');
  },

  async switchTab(tabName) {
    this.currentTab = tabName;

    // Update nav link active styles
    document.querySelectorAll('.sidebar-nav button').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
    });

    // Update topbar title
    const topbarTitle = document.getElementById('topbar-title');
    if (topbarTitle) {
      topbarTitle.textContent = tabName.charAt(0).toUpperCase() + tabName.slice(1) + ' Management';
    }

    // Render tab view
    const mainView = document.getElementById('admin-main-view');
    mainView.innerHTML = '<div style="padding: 2rem; text-align: center;">Loading ' + tabName + '...</div>';

    switch (tabName) {
      case 'dashboard': await this.renderDashboardView(mainView); break;
      case 'sections': await this.renderSectionsView(mainView); break;
      case 'projects': await this.renderProjectsView(mainView); break;
      case 'skills': await this.renderSkillsView(mainView); break;
      case 'experience': await this.renderExperienceView(mainView); break;
      case 'achievements': await this.renderAchievementsView(mainView); break;
      case 'blogs': await this.renderBlogsView(mainView); break;
      default: mainView.innerHTML = '<p>View coming soon.</p>';
    }
  },

  async renderDashboardView(container) {
    container.innerHTML = `
      <div class="admin-card">
        <div class="admin-card-header">
          <h2 class="admin-card-title">Publishing & Status Dashboard</h2>
          <span class="badge badge-published">ONLINE</span>
        </div>
        <p style="color: var(--text-sub); margin-bottom: 1.5rem;">
          Edit portfolio content across sections. Once ready, click <strong>PUBLISH TO WEBSITE</strong> to update published JSON files atomically.
        </p>
        <button id="dashboard-publish-btn" class="publish-cta-btn" style="padding: 0.85rem 1.8rem; font-size: 1rem;">
          🚀 Publish Site Now
        </button>
      </div>
    `;

    document.getElementById('dashboard-publish-btn').addEventListener('click', () => this.handlePublish());
  },

  async renderSectionsView(container) {
    let sections = [];
    try { sections = await AdminAPI.getSections(); } catch(e) { sections = []; }

    container.innerHTML = `
      <div class="admin-card">
        <div class="admin-card-header">
          <h2 class="admin-card-title">Manage Dynamic Sections</h2>
          <span style="color: var(--text-sub); font-size: 0.85rem;">Reorder, toggle theme, and manage visibility</span>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Title</th>
              <th>Type</th>
              <th>Theme</th>
              <th>Visible</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${sections.length ? sections.map(s => `
              <tr>
                <td><strong>#${s.order}</strong></td>
                <td>${s.title}</td>
                <td><span class="badge badge-published">${s.type}</span></td>
                <td><span class="badge" style="background: rgba(139,92,246,0.2); color:#8b5cf6;">${s.theme || 'default'}</span></td>
                <td>${s.visible ? '✅ Visible' : '🙈 Hidden'}</td>
                <td>
                  <button class="btn-admin btn-admin-secondary">Edit</button>
                </td>
              </tr>
            `).join('') : '<tr><td colspan="6">No dynamic sections found. Default section structure will be published.</td></tr>'}
          </tbody>
        </table>
      </div>
    `;
  },

  async renderProjectsView(container) {
    container.innerHTML = `
      <div class="admin-card">
        <div class="admin-card-header">
          <h2 class="admin-card-title">Portfolio Projects</h2>
          <button class="btn-admin btn-admin-primary">+ Add New Project</button>
        </div>
        <p style="color: var(--text-sub);">Manage featured projects, repository links, tags, and cover images.</p>
      </div>
    `;
  },

  async renderSkillsView(container) {
    container.innerHTML = `<div class="admin-card"><h2>Skills & Tech Stack Manager</h2><p style="color: var(--text-sub);">Manage technical skills, proficiency levels, and categories.</p></div>`;
  },
  async renderExperienceView(container) {
    container.innerHTML = `<div class="admin-card"><h2>Work Experience Manager</h2><p style="color: var(--text-sub);">Manage timeline items, company roles, and bullet highlights.</p></div>`;
  },
  async renderAchievementsView(container) {
    container.innerHTML = `<div class="admin-card"><h2>Key Achievements Manager</h2><p style="color: var(--text-sub);">Manage numerical metric cards and key highlights.</p></div>`;
  },
  async renderBlogsView(container) {
    container.innerHTML = `<div class="admin-card"><h2>Writing & Blog Posts Manager</h2><p style="color: var(--text-sub);">Draft articles, edit Markdown summary, and manage published posts.</p></div>`;
  },

  async handlePublish() {
    const btn = document.getElementById('publish-site-btn');
    if (btn) btn.disabled = true;

    try {
      alert("Initiating atomic publishing process...\n\n1. Compiling draft JSON\n2. Uploading versioned JSON files\n3. Verifying uploads\n4. Updating manifest.json LAST");
      const res = await AdminAPI.publish();
      alert(`Publication Successful! 🎉\n\nVersion: v${res.version || 1}\nManifest updated successfully.`);
    } catch (err) {
      alert(`Publishing Failed: ${err.message}\n\nManifest was NOT updated. Previous published version remains active.`);
    } finally {
      if (btn) btn.disabled = false;
    }
  }
};
