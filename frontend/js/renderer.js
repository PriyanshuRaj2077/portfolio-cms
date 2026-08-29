/* ==========================================================================
   DYNAMIC SECTION RENDERER & SANITIZATION ENGINE
   Editorial Layout & Markdown Pipeline
   ========================================================================== */

const Renderer = {
  /**
   * Client-side text sanitizer to prevent XSS in simple strings
   */
  sanitizeHTML(dirty) {
    if (!dirty) return '';
    const temp = document.createElement('div');
    temp.textContent = String(dirty);
    return temp.innerHTML;
  },

  /**
   * Decode HTML entities in strings without corrupting valid parameters
   */
  decodeHtmlEntities(str) {
    if (!str || typeof str !== 'string') return '';
    return str
      .replace(/&amp;/gi, '&')
      .replace(/&quot;/gi, '"')
      .replace(/&#39;|&#x27;|&apos;/gi, "'")
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>')
      .replace(/&#(\d+);/g, (_, num) => String.fromCharCode(parseInt(num, 10)))
      .replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)));
  },

  /**
   * Centralized safe URL sanitizer
   * Handles:
   * - Relative paths (media/..., ./media/..., ../media/..., /media/...) -> normalized to root-relative (/...)
   * - Absolute URLs (https://..., http://..., mailto:..., //...)
   * - Supabase Storage URLs with signatures, tokens, and query parameters
   * - HTML entities (&amp;, &#38;, etc.) decoded cleanly without corrupting parameters
   * - Special characters (parentheses, @2x, dashes, underscores, encoded spaces)
   * - Strips surrounding quotes, angle brackets, and markdown titles
   * Rejects: javascript:, vbscript:, data:, file:, blob:, and dangerous injection vectors
   */
  sanitizeUrl(url) {
    if (!url || typeof url !== 'string') return '#';
    let trimmed = url.trim();
    if (!trimmed) return '#';

    // 1. Decode HTML entities (e.g. &amp; -> &, &quot; -> ", etc.)
    trimmed = this.decodeHtmlEntities(trimmed);

    // 2. Strip trailing markdown link title if passed with URL (e.g., 'url "title"' or "url 'title'")
    trimmed = trimmed.replace(/\s+["'][^"']*["']\s*$/, '').trim();

    // 3. Strip surrounding angle brackets <url> and quotes "url" / 'url'
    trimmed = trimmed.replace(/^<|>$/g, '').replace(/^["']|["']$/g, '').trim();

    // 4. Encode unescaped whitespace to %20 to avoid broken URLs while maintaining safety
    trimmed = trimmed.replace(/\s+/g, '%20');

    // 5. Reject dangerous control characters or characters that break attribute boundaries
    if (/[\r\n\t<>"'`]/.test(trimmed)) {
      return '#';
    }

    // 6. Fragment links
    if (trimmed.startsWith('#')) {
      return trimmed;
    }

    // 7. Check for protocol schemes
    const schemeMatch = trimmed.match(/^([a-zA-Z0-9+.-]+):/);
    if (schemeMatch) {
      const scheme = schemeMatch[1].toLowerCase();
      if (scheme === 'https' || scheme === 'http' || scheme === 'mailto') {
        return trimmed;
      }
      // Reject javascript:, data:, vbscript:, file:, blob:, etc.
      return '#';
    }

    // 8. Protocol-relative URLs (//cdn.example.com/...)
    if (trimmed.startsWith('//')) {
      return trimmed;
    }

    // 9. Relative paths: normalize to root-relative path (e.g. media/photo.png, ./media/photo.png, ../media/photo.png -> /media/photo.png)
    // This ensures images resolve consistently regardless of route (/ or /blog/<slug>) or refresh.
    let normalized = trimmed;
    if (normalized.startsWith('./')) {
      normalized = normalized.substring(2);
    }
    while (normalized.startsWith('../')) {
      normalized = normalized.substring(3);
    }
    if (!normalized.startsWith('/')) {
      normalized = '/' + normalized;
    }

    return normalized;
  },

  /**
   * HTML Sanitizer using browser DOMParser.
   * Strips dangerous tags (<script>, <style>, <iframe>, <object>, <embed>, <form>, <input>, etc.),
   * all inline event handlers (onload, onerror, onclick, etc.),
   * and unsafe URI schemes (javascript:, data:text/html, vbscript:).
   * Normalizes and validates image src and anchor href URLs.
   */
  sanitizeRenderedHTML(htmlString) {
    if (!htmlString) return '';
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlString, 'text/html');

    const allowedTags = new Set([
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'br', 'hr',
      'strong', 'b', 'em', 'i', 'u', 's', 'del', 'strike',
      'code', 'pre', 'blockquote', 'ul', 'ol', 'li',
      'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'span', 'div'
    ]);

    const allowedAttributes = {
      'a': ['href', 'title', 'target', 'rel', 'class'],
      'img': ['src', 'alt', 'title', 'width', 'height', 'loading', 'class'],
      'code': ['class'],
      'pre': ['class'],
      'th': ['align', 'class'],
      'td': ['align', 'class'],
      'span': ['class'],
      'div': ['class']
    };

    const sanitizeNode = (node) => {
      const children = Array.from(node.childNodes || []);
      for (const child of children) {
        if (child.nodeType === Node.ELEMENT_NODE) {
          const tagName = (child.tagName || '').toLowerCase();
          if (!allowedTags.has(tagName)) {
            child.remove();
            continue;
          }

          const attrs = Array.from(child.attributes || []);
          const validAttrs = allowedAttributes[tagName] || [];
          for (const attr of attrs) {
            const attrName = (attr.name || (Array.isArray(attr) ? attr[0] : '')).toLowerCase();
            const attrValue = attr.value !== undefined ? attr.value : (Array.isArray(attr) ? attr[1] : (child.getAttribute(attrName) || ''));

            if (attrName.startsWith('on') || !validAttrs.includes(attrName)) {
              child.removeAttribute(attrName);
            } else if (attrName === 'src' && tagName === 'img') {
              const safeSrc = Renderer.sanitizeUrl(attrValue);
              if (safeSrc && safeSrc !== '#') {
                child.setAttribute('src', safeSrc);
                if (!child.hasAttribute('loading')) {
                  child.setAttribute('loading', 'lazy');
                }
              } else {
                child.removeAttribute('src');
              }
            } else if (attrName === 'href' && tagName === 'a') {
              const safeHref = Renderer.sanitizeUrl(attrValue);
              if (safeHref && safeHref !== '#') {
                child.setAttribute('href', safeHref);
              } else {
                child.removeAttribute('href');
              }
            }
          }

          if (tagName === 'a' && child.hasAttribute('href')) {
            child.setAttribute('target', '_blank');
            child.setAttribute('rel', 'noopener noreferrer');
          }

          sanitizeNode(child);
        } else if (child.nodeType === Node.COMMENT_NODE) {
          child.remove();
        }
      }
    };

    sanitizeNode(doc.body);
    return doc.body.innerHTML;
  },

  /**
   * Markdown Parser to HTML Pipeline with Sanitization
   * Markdown -> Parser -> HTML -> Sanitizer -> Safe HTML
   */
  renderMarkdown(markdown) {
    if (!markdown) return '';
    let md = String(markdown);

    // Normalize newlines
    md = md.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

    // Code blocks
    const codeBlocks = [];
    md = md.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
      const index = codeBlocks.length;
      const escapedCode = code
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      codeBlocks.push(`<pre><code class="language-${lang}">${escapedCode}</code></pre>`);
      return `@@CODEBLOCK_${index}@@`;
    });

    // Inline code
    const inlineCodes = [];
    md = md.replace(/`([^`\n]+)`/g, (match, code) => {
      const index = inlineCodes.length;
      const escapedCode = code
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      inlineCodes.push(`<code>${escapedCode}</code>`);
      return `@@INLINECODE_${index}@@`;
    });

    // Headings (H6 down to H1)
    md = md.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>');
    md = md.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>');
    md = md.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>');
    md = md.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>');
    md = md.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>');
    md = md.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>');

    // Horizontal Rules
    md = md.replace(/^(?:---|\*\*\*|___)\s*$/gm, '<hr>');

    // Blockquotes
    md = md.replace(/^>\s+(.+)$/gm, '<blockquote><p>$1</p></blockquote>');

    // Bold & Italic
    md = md.replace(/\*\*\*([^*]+)\*\*\*/g, '<strong><em>$1</em></strong>');
    md = md.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    md = md.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    md = md.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    md = md.replace(/_([^_]+)_/g, '<em>$1</em>');

    // Images & Links (supporting parentheses in URLs, spaces, special chars, optional titles)
    md = md.replace(/!\[([^\]]*)\]\((<[^>]+>|(?:[^\s()]|\([^\s()]*\))+)(?:\s+["']([^"']*)["'])?\)/g, (match, alt, url, title) => {
      const safeUrl = this.sanitizeUrl(url.trim());
      const safeAlt = this.sanitizeHTML(alt);
      const titleAttr = title ? ` title="${this.sanitizeHTML(title)}"` : '';
      return `<img src="${safeUrl}" alt="${safeAlt}"${titleAttr} loading="lazy">`;
    });
    md = md.replace(/\[([^\]]+)\]\((<[^>]+>|(?:[^\s()]|\([^\s()]*\))+)(?:\s+["']([^"']*)["'])?\)/g, (match, text, url, title) => {
      const safeUrl = this.sanitizeUrl(url.trim());
      const safeText = this.sanitizeHTML(text);
      const titleAttr = title ? ` title="${this.sanitizeHTML(title)}"` : '';
      return `<a href="${safeUrl}"${titleAttr}>${safeText}</a>`;
    });

    // Unordered Lists
    md = md.replace(/^[\*\-]\s+(.+)$/gm, '<li>$1</li>');
    md = md.replace(/(<li>[\s\S]*?<\/li>)/g, (match) => `<ul>${match}</ul>`);
    md = md.replace(/<\/ul>\s*<ul>/g, '');

    // Ordered Lists
    md = md.replace(/^\d+\.\s+(.+)$/gm, '<oli>$1</oli>');
    md = md.replace(/(<oli>[\s\S]*?<\/oli>)/g, (match) => {
      const fixed = match.replace(/<\/?oli>/g, tag => tag.replace('oli', 'li'));
      return `<ol>${fixed}</ol>`;
    });
    md = md.replace(/<\/ol>\s*<ol>/g, '');

    // Paragraphs for remaining text blocks
    const lines = md.split(/\n\n+/);
    const parsedBlocks = lines.map(block => {
      const trimmed = block.trim();
      if (!trimmed) return '';
      if (/^(<h[1-6]|<pre|<blockquote|<ul|<ol|<hr|<img|<div|<p|@@CODEBLOCK)/.test(trimmed)) {
        return trimmed;
      }
      return `<p>${trimmed.replace(/\n/g, '<br>')}</p>`;
    });
    md = parsedBlocks.filter(Boolean).join('\n\n');

    // Restore code blocks and inline codes
    md = md.replace(/@@CODEBLOCK_(\d+)@@/g, (match, idx) => codeBlocks[Number(idx)] || '');
    md = md.replace(/@@INLINECODE_(\d+)@@/g, (match, idx) => inlineCodes[Number(idx)] || '');

    // Pass through HTML sanitizer
    return this.sanitizeRenderedHTML(md);
  },

  /**
   * Strict single uppercase navigation letter resolver
   * section.navLetter -> if missing, first character of section.title -> uppercase [A-Z]
   * Ignores legacy icon field.
   */
  getSectionNavLetter(section) {
    if (!section) return 'S';
    let raw = '';
    if (section.navLetter && typeof section.navLetter === 'string') {
      const match = section.navLetter.trim().match(/[a-zA-Z]/);
      if (match) {
        raw = match[0];
      }
    }
    if (!raw && section.title && typeof section.title === 'string') {
      const match = section.title.trim().match(/[a-zA-Z]/);
      if (match) {
        raw = match[0];
      }
    }
    return (raw || 'S').toUpperCase();
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

  /**
   * Dynamically normalizes skills data into categories:
   * - Handles flat SkillEntity[] from API/Supabase/published JSON
   * - Handles grouped {category, skills[]} arrays (like default fallback skills.v1.json)
   * - Handles category object maps { "Category": [...] }
   * - Preserves skill names, categories, and sort order without any hardcoding
   */
  normalizeSkills(skillsData) {
    if (!skillsData) return [];

    // Case 1: Object dictionary { "Category": [...] }
    if (!Array.isArray(skillsData) && typeof skillsData === 'object') {
      const categories = [];
      for (const [catName, list] of Object.entries(skillsData)) {
        if (!catName || !list) continue;
        const skillList = Array.isArray(list) ? list : [list];
        const names = skillList
          .map(s => (typeof s === 'string' ? s.trim() : (s && (s.name || s.skill || s.title) ? String(s.name || s.skill || s.title).trim() : '')))
          .filter(Boolean);
        if (names.length > 0) {
          categories.push({ category: catName.trim(), skills: names });
        }
      }
      return categories;
    }

    if (!Array.isArray(skillsData) || skillsData.length === 0) return [];

    // Check if the array elements are already grouped: item has a 'skills' array property
    const isGrouped = skillsData.some(item => item && Array.isArray(item.skills));

    if (isGrouped) {
      return skillsData.map(cat => {
        if (!cat) return null;
        const catTitle = cat.category || cat.title || cat.name || 'General';
        const rawSkills = Array.isArray(cat.skills) ? cat.skills : [];
        const names = rawSkills
          .map(s => (typeof s === 'string' ? s.trim() : (s && (s.name || s.skill || s.title) ? String(s.name || s.skill || s.title).trim() : '')))
          .filter(Boolean);
        return { category: String(catTitle).trim(), skills: names };
      }).filter(c => c && c.skills.length > 0);
    }

    // Case 2: Flat SkillEntity[] array (e.g. from API/Supabase: [{ id, name, category, level, sortOrder }, ...])
    const sorted = [...skillsData].sort((a, b) => {
      const orderA = (a && typeof a.sortOrder === 'number') ? a.sortOrder : 0;
      const orderB = (b && typeof b.sortOrder === 'number') ? b.sortOrder : 0;
      return orderA - orderB;
    });

    const categoryMap = new Map();

    for (const item of sorted) {
      if (!item) continue;
      const rawName = item.name || item.skill || item.title || (typeof item === 'string' ? item : '');
      const skillName = String(rawName).trim();
      if (!skillName) continue;

      const catName = String(item.category || item.categoryName || 'General').trim();
      if (!categoryMap.has(catName)) {
        categoryMap.set(catName, []);
      }
      categoryMap.get(catName).push(skillName);
    }

    const result = [];
    for (const [category, skills] of categoryMap.entries()) {
      if (skills.length > 0) {
        result.push({ category, skills });
      }
    }
    return result;
  },

  // 3. Skills / Tech Stack Renderer
  renderSkills(skillsData) {
    const normalized = this.normalizeSkills(skillsData);
    if (!normalized || normalized.length === 0) return '<p>No tech stack published yet.</p>';
    return `
      <div class="techstack-editorial">
        ${normalized.map(cat => `
          <div class="techstack-category">
            <h3 class="techstack-cat-title">${this.sanitizeHTML(cat.category)}</h3>
            <div class="techstack-items-grid">
              ${cat.skills.map(skillName => `
                <span class="techstack-item">${this.sanitizeHTML(skillName)}</span>
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
      <div class="projects-editorial-list">
        ${projects.map((p, idx) => {
          const num = String(idx + 1).padStart(2, '0');
          let tags = [];
          if (Array.isArray(p.tags)) {
            tags = p.tags;
          } else if (typeof p.tagsJson === 'string') {
            try { tags = JSON.parse(p.tagsJson); } catch (e) { tags = p.tagsJson.split(','); }
          }
          const primaryUrl = p.liveUrl || p.repoUrl || '#';
          return `
            <div class="project-editorial-item">
              <div class="project-num">${num}</div>
              <div class="project-details">
                <div>
                  <a href="${this.sanitizeUrl(primaryUrl)}" target="_blank" rel="noopener noreferrer" class="project-title-link">
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
                ${p.repoUrl ? `<a href="${this.sanitizeUrl(p.repoUrl)}" target="_blank" rel="noopener noreferrer" class="project-action-link">GitHub ↗</a>` : ''}
                ${p.liveUrl ? `<a href="${this.sanitizeUrl(p.liveUrl)}" target="_blank" rel="noopener noreferrer" class="project-action-link">Live ↗</a>` : ''}
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
          const slug = b.slug || b.id || idx;
          return `
            <div class="blog-editorial-item" data-blog-slug="${this.sanitizeHTML(slug)}">
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
    const email = profile.email;
    const githubUrl = profile.githubUrl || social.github;
    const linkedinUrl = profile.linkedinUrl || social.linkedin;
    const twitterUrl = profile.twitterUrl || social.twitter;

    return `
      <div class="contact-editorial">
        <p class="contact-intro-text">${this.sanitizeHTML(profile.bio || 'Reach out directly for collaborations or discussions.')}</p>
        <div class="contact-links-list">
          ${email ? `
            <a href="${this.sanitizeUrl(`mailto:${email}`)}" class="contact-item-link">
              <span>Email</span>
              <span>${this.sanitizeHTML(email)} ↗</span>
            </a>
          ` : ''}
          ${githubUrl ? `
            <a href="${this.sanitizeUrl(githubUrl)}" target="_blank" rel="noopener noreferrer" class="contact-item-link">
              <span>GitHub</span>
              <span>Profile ↗</span>
            </a>
          ` : ''}
          ${linkedinUrl ? `
            <a href="${this.sanitizeUrl(linkedinUrl)}" target="_blank" rel="noopener noreferrer" class="contact-item-link">
              <span>LinkedIn</span>
              <span>Profile ↗</span>
            </a>
          ` : ''}
          ${twitterUrl ? `
            <a href="${this.sanitizeUrl(twitterUrl)}" target="_blank" rel="noopener noreferrer" class="contact-item-link">
              <span>Twitter / X</span>
              <span>Profile ↗</span>
            </a>
          ` : ''}
        </div>
      </div>
    `;
  },

  // 7. Custom Text Section Renderer
  renderText(content) {
    return `<div class="editorial-text">${this.renderMarkdown(content)}</div>`;
  },

  // 8. Gallery Renderer
  renderGallery(images) {
    if (!images || images.length === 0) return '<p>No gallery items published yet.</p>';
    return `<div class="techstack-items-grid">${images.map(img => `<span>${this.sanitizeHTML(img.caption || img.url)} ${img.url ? `<a href="${this.sanitizeUrl(img.url)}" target="_blank" rel="noopener noreferrer">↗</a>` : ''}</span>`).join('')}</div>`;
  }
};
