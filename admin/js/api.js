/* ==========================================================================
   ADMIN API CLIENT
   REST Communications with Spring Boot Backend Engine
   ========================================================================== */

const AdminAPI = {
  baseUrl: '/api/admin',

  async request(endpoint, options = {}) {
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const config = {
      method: options.method || 'GET',
      headers: { ...defaultHeaders, ...options.headers },
      credentials: 'include', // Include HttpOnly JSESSIONID session cookie
      ...options
    };

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      config.body = JSON.stringify(options.body);
    }

    try {
      const response = await fetch(`${this.baseUrl}${endpoint}`, config);

      if (response.status === 401 || response.status === 403) {
        // Trigger login modal/view if unauthenticated
        AdminApp.showLogin();
        throw new Error('Unauthorized or session expired');
      }

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API Error (${response.status}): ${errorText}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      return await response.text();
    } catch (err) {
      console.error(`API Exception on ${endpoint}:`, err);
      throw err;
    }
  },

  // Auth Operations
  async checkSession() {
    return this.request('/auth/check');
  },

  async login(username, password) {
    return this.request('/auth/login', {
      method: 'POST',
      body: { username, password }
    });
  },

  async logout() {
    return this.request('/auth/logout', { method: 'POST' });
  },

  // Content CRUD Operations
  async getProfile() { return this.request('/profile'); },
  async updateProfile(data) { return this.request('/profile', { method: 'PUT', body: data }); },

  async getSections() { return this.request('/sections'); },
  async saveSection(section) { return this.request('/sections', { method: 'POST', body: section }); },
  async deleteSection(id) { return this.request(`/sections/${id}`, { method: 'DELETE' }); },

  async getProjects() { return this.request('/projects'); },
  async saveProject(project) { return this.request('/projects', { method: 'POST', body: project }); },
  async deleteProject(id) { return this.request(`/projects/${id}`, { method: 'DELETE' }); },

  async getSkills() { return this.request('/skills'); },
  async saveSkill(skill) { return this.request('/skills', { method: 'POST', body: skill }); },
  async deleteSkill(id) { return this.request(`/skills/${id}`, { method: 'DELETE' }); },

  async getExperience() { return this.request('/experience'); },
  async saveExperience(exp) { return this.request('/experience', { method: 'POST', body: exp }); },
  async deleteExperience(id) { return this.request(`/experience/${id}`, { method: 'DELETE' }); },

  async getAchievements() { return this.request('/achievements'); },
  async saveAchievement(ach) { return this.request('/achievements', { method: 'POST', body: ach }); },
  async deleteAchievement(id) { return this.request(`/achievements/${id}`, { method: 'DELETE' }); },

  async getBlogs() { return this.request('/blogs'); },
  async saveBlog(blog) { return this.request('/blogs', { method: 'POST', body: blog }); },
  async deleteBlog(id) { return this.request(`/blogs/${id}`, { method: 'DELETE' }); },

  // Publish Operation (Atomic 2-phase publication)
  async publish() {
    return this.request('/publish', { method: 'POST' });
  }
};
