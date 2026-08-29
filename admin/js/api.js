/* ==========================================================================
   ADMIN API CLIENT
   REST Communications with Spring Boot Backend Engine
   ========================================================================== */

const AdminAPI = {
  baseUrl: '/api/admin',
  csrfToken: null,

  getCookie(name) {
    const match = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'));
    return match ? decodeURIComponent(match[2]) : null;
  },

  getCsrfToken() {
    return this.getCookie('XSRF-TOKEN') || this.csrfToken;
  },

  async ensureCsrfToken() {
    let token = this.getCsrfToken();
    if (token) return token;

    try {
      const res = await fetch(`${this.baseUrl}/auth/csrf`, {
        method: 'GET',
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        if (data && data.token) {
          this.csrfToken = data.token;
          return data.token;
        }
      }
    } catch (e) {
      console.warn('CSRF bootstrap fetch failed:', e);
    }
    return this.getCsrfToken();
  },

  async request(endpoint, options = {}) {
    const isFormData = options.body instanceof FormData;
    const defaultHeaders = isFormData
      ? { 'Accept': 'application/json' }
      : {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        };

    const method = (options.method || 'GET').toUpperCase();
    const headers = { ...defaultHeaders, ...(options.headers || {}) };

    // Attach CSRF token on mutating methods (POST, PUT, PATCH, DELETE)
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
      let token = this.getCsrfToken();
      if (!token) {
        token = await this.ensureCsrfToken();
      }
      if (token) {
        headers['X-XSRF-TOKEN'] = token;
      }
    }

    const config = {
      method: method,
      headers: headers,
      credentials: 'include', // Include HttpOnly JSESSIONID session cookie
      ...options
    };

    if (options.body && typeof options.body === 'object' && !isFormData) {
      config.body = JSON.stringify(options.body);
    }

    try {
      const response = await fetch(`${this.baseUrl}${endpoint}`, config);

      // Keep token updated from cookie if changed
      const cookieToken = this.getCookie('XSRF-TOKEN');
      if (cookieToken) {
        this.csrfToken = cookieToken;
      }

      if (response.status === 401 || response.status === 403) {
        if (window.AdminApp && endpoint !== '/auth/login') {
          AdminApp.showLogin();
        }
      }

      const contentType = response.headers.get('content-type') || '';
      let data = null;
      if (contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (data && data.csrfToken) {
        this.csrfToken = data.csrfToken;
      }

      if (!response.ok) {
        const errorMsg = (data && data.error) ? data.error : ((data && data.message) ? data.message : (typeof data === 'string' ? data : `Error (${response.status})`));
        throw new Error(errorMsg);
      }

      return data;
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

  // Media Library Operations
  async getMedia() { return this.request('/media'); },
  async uploadMedia(file) {
    const formData = new FormData();
    formData.append('file', file);
    return this.request('/media/upload', {
      method: 'POST',
      body: formData
    });
  },
  async deleteMedia(id) { return this.request(`/media/${id}`, { method: 'DELETE' }); },

  // Comment Management Operations (Admin)
  async getComments() { return this.request('/comments'); },
  async approveComment(id) { return this.request(`/comments/${id}/approve`, { method: 'POST' }); },
  async deleteComment(id) { return this.request(`/comments/${id}`, { method: 'DELETE' }); },

  // Publish Operation (Atomic 2-phase publication)
  async publish() {
    return this.request('/publish', { method: 'POST' });
  }
};

