/**
 * Comprehensive Frontend Verification Test Suite
 * Tests:
 * 1. Tech Stack layout & dynamic normalization (API/CMS SkillEntity flat list, grouped fallback, object map, edge cases)
 * 2. Markdown Image pipeline (![alt](url), titles, relative paths, Supabase URLs, tokens, &, query params, spaces, parentheses, underscores)
 * 3. Mobile Navigation (Fixed compact bottom bar, 4 plain text shortcuts, responsive media queries, safe area insets)
 */

const fs = require('fs');
const assert = require('assert');

// Mock browser DOM environment if running directly under Node.js
if (typeof document === 'undefined') {
  global.document = {
    createElement: (tag) => {
      const element = {
        tagName: tag.toUpperCase(),
        attributes: {},
        childNodes: [],
        id: '',
        className: '',
        style: {},
        innerHTML: '',
        setAttribute(k, v) { this.attributes[k] = String(v); },
        getAttribute(k) { return this.attributes[k]; },
        hasAttribute(k) { return this.attributes[k] !== undefined; },
        removeAttribute(k) { delete this.attributes[k]; },
        appendChild(child) { this.childNodes.push(child); return child; },
        set textContent(v) {
          this._text = String(v);
          this.innerHTML = String(v)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
        },
        get textContent() { return this._text || ''; }
      };
      return element;
    }
  };
}

if (typeof Node === 'undefined') {
  global.Node = { ELEMENT_NODE: 1, COMMENT_NODE: 8 };
}

if (typeof DOMParser === 'undefined') {
  global.DOMParser = function() {
    return {
      parseFromString: (htmlString, type) => {
        return {
          body: {
            innerHTML: htmlString,
            childNodes: []
          }
        };
      }
    };
  };
}

// Load Renderer and App
let rendererCode = fs.readFileSync('frontend/js/renderer.js', 'utf8') + '; global.Renderer = Renderer;';
eval(rendererCode);

let passedCount = 0;
let totalCount = 0;

function test(name, fn) {
  totalCount++;
  try {
    fn();
    console.log(`  ✓ ${name}`);
    passedCount++;
  } catch (err) {
    console.error(`  ✗ ${name}`);
    console.error(`    ${err.message}`);
    throw err;
  }
}

console.log('\n======================================================');
console.log('1. TECH STACK LAYOUT & SKILL NORMALIZATION TESTS');
console.log('======================================================');

test('Normalizes flat SkillEntity[] from API/Supabase/PublishService into category groups', () => {
  const flatSkills = [
    { id: 1, name: 'Java', category: 'Programming', sortOrder: 1 },
    { id: 2, name: 'Python', category: 'Programming', sortOrder: 2 },
    { id: 3, name: 'JavaScript', category: 'Programming', sortOrder: 3 },
    { id: 4, name: 'SQL', category: 'Programming', sortOrder: 4 },
    { id: 5, name: 'Spring Boot', category: 'Backend', sortOrder: 5 },
    { id: 6, name: 'REST APIs', category: 'Backend', sortOrder: 6 },
    { id: 7, name: 'Spring Security', category: 'Backend', sortOrder: 7 },
    { id: 8, name: 'HTML', category: 'Frontend', sortOrder: 8 },
    { id: 9, name: 'CSS', category: 'Frontend', sortOrder: 9 },
    { id: 10, name: 'PostgreSQL', category: 'Database', sortOrder: 10 },
    { id: 11, name: 'MySQL', category: 'Database', sortOrder: 11 },
    { id: 12, name: 'Git', category: 'Tools', sortOrder: 12 },
    { id: 13, name: 'GitHub', category: 'Tools', sortOrder: 13 },
    { id: 14, name: 'Docker', category: 'Tools', sortOrder: 14 },
    { id: 15, name: 'Linux', category: 'Tools', sortOrder: 15 }
  ];

  const normalized = Renderer.normalizeSkills(flatSkills);
  assert.strictEqual(normalized.length, 5);
  assert.strictEqual(normalized[0].category, 'Programming');
  assert.deepStrictEqual(normalized[0].skills, ['Java', 'Python', 'JavaScript', 'SQL']);
  assert.strictEqual(normalized[1].category, 'Backend');
  assert.deepStrictEqual(normalized[1].skills, ['Spring Boot', 'REST APIs', 'Spring Security']);
  assert.strictEqual(normalized[2].category, 'Frontend');
  assert.deepStrictEqual(normalized[2].skills, ['HTML', 'CSS']);
  assert.strictEqual(normalized[3].category, 'Database');
  assert.deepStrictEqual(normalized[3].skills, ['PostgreSQL', 'MySQL']);
  assert.strictEqual(normalized[4].category, 'Tools');
  assert.deepStrictEqual(normalized[4].skills, ['Git', 'GitHub', 'Docker', 'Linux']);
});

test('Renders Tech Stack HTML matching editorial CSS classes and structure', () => {
  const flatSkills = [
    { id: 1, name: 'Java', category: 'Programming', sortOrder: 1 },
    { id: 2, name: 'Python', category: 'Programming', sortOrder: 2 },
    { id: 3, name: 'Spring Boot', category: 'Backend', sortOrder: 3 }
  ];

  const html = Renderer.renderSkills(flatSkills);
  assert.ok(html.includes('class="techstack-editorial"'));
  assert.ok(html.includes('class="techstack-category"'));
  assert.ok(html.includes('class="techstack-cat-title">Programming</h3>'));
  assert.ok(html.includes('class="techstack-items-grid"'));
  assert.ok(html.includes('class="techstack-item">Java</span>'));
  assert.ok(html.includes('class="techstack-item">Python</span>'));
  assert.ok(html.includes('class="techstack-cat-title">Backend</h3>'));
  assert.ok(html.includes('class="techstack-item">Spring Boot</span>'));
});

test('Handles grouped fallback skills.v1.json structure', () => {
  const groupedSkills = [
    {
      category: 'Languages & Core',
      skills: [
        { name: 'Java 21', level: 'Expert' },
        { name: 'JavaScript (ES6+)', level: 'Expert' }
      ]
    },
    {
      category: 'Backend & Storage',
      skills: [
        { name: 'Spring Boot 3', level: 'Expert' }
      ]
    }
  ];

  const normalized = Renderer.normalizeSkills(groupedSkills);
  assert.strictEqual(normalized.length, 2);
  assert.strictEqual(normalized[0].category, 'Languages & Core');
  assert.deepStrictEqual(normalized[0].skills, ['Java 21', 'JavaScript (ES6+)']);
  assert.strictEqual(normalized[1].category, 'Backend & Storage');
  assert.deepStrictEqual(normalized[1].skills, ['Spring Boot 3']);
});

test('Handles object map category structure { "Category": [...] }', () => {
  const mapSkills = {
    'Programming': ['Java', 'Python'],
    'Database': [{ name: 'PostgreSQL' }]
  };
  const normalized = Renderer.normalizeSkills(mapSkills);
  assert.strictEqual(normalized.length, 2);
  assert.strictEqual(normalized[0].category, 'Programming');
  assert.deepStrictEqual(normalized[0].skills, ['Java', 'Python']);
  assert.strictEqual(normalized[1].category, 'Database');
  assert.deepStrictEqual(normalized[1].skills, ['PostgreSQL']);
});

test('Gracefully handles empty or null skills data', () => {
  assert.strictEqual(Renderer.renderSkills([]), '<p>No tech stack published yet.</p>');
  assert.strictEqual(Renderer.renderSkills(null), '<p>No tech stack published yet.</p>');
  assert.strictEqual(Renderer.renderSkills(undefined), '<p>No tech stack published yet.</p>');
});


console.log('\n======================================================');
console.log('2. INLINE ARTICLE MARKDOWN IMAGES PIPELINE TESTS');
console.log('======================================================');

test('Renders Supabase URL with underscores without corruption from italic parser', () => {
  const md = '![Architecture](https://abc.supabase.co/storage/v1/object/public/media/how_i_built_a_portfolio_cms.png)';
  const html = Renderer.renderMarkdown(md);
  assert.ok(html.includes('<img src="https://abc.supabase.co/storage/v1/object/public/media/how_i_built_a_portfolio_cms.png"'));
  assert.ok(html.includes('alt="Architecture"'));
  assert.ok(html.includes('loading="lazy"'));
  assert.ok(!html.includes('<em>'));
  assert.ok(!html.includes('src="#"'));
});

test('Renders Supabase URLs with tokens, query parameters, & and timestamps', () => {
  const md = '![Token Test](https://xyz.supabase.co/storage/v1/object/public/media/image.png?token=eyJhbGciOi_secret_jwt&v=2&t=2026-08-29T10:00:00Z)';
  const html = Renderer.renderMarkdown(md);
  assert.ok(html.includes('<img src="https://xyz.supabase.co/storage/v1/object/public/media/image.png?token=eyJhbGciOi_secret_jwt&v=2&t=2026-08-29T10:00:00Z"'));
  assert.ok(html.includes('alt="Token Test"'));
});

test('Normalizes relative paths to root-relative paths', () => {
  const md1 = '![Relative 1](media/photo.png)';
  const html1 = Renderer.renderMarkdown(md1);
  assert.ok(html1.includes('<img src="/media/photo.png" alt="Relative 1" loading="lazy">'));

  const md2 = '![Relative 2](./media/diagram.png)';
  const html2 = Renderer.renderMarkdown(md2);
  assert.ok(html2.includes('<img src="/media/diagram.png" alt="Relative 2" loading="lazy">'));

  const md3 = '![Relative 3](../media/assets/flow.png)';
  const html3 = Renderer.renderMarkdown(md3);
  assert.ok(html3.includes('<img src="/media/assets/flow.png" alt="Relative 3" loading="lazy">'));
});

test('Supports spaces in image URLs (both with and without angle brackets)', () => {
  const md1 = '![Spaces 1](media/system architecture overview.png)';
  const html1 = Renderer.renderMarkdown(md1);
  assert.ok(html1.includes('<img src="/media/system%20architecture%20overview.png" alt="Spaces 1" loading="lazy">'));

  const md2 = '![Spaces 2](<media/system architecture overview.png> "System Architecture")';
  const html2 = Renderer.renderMarkdown(md2);
  assert.ok(html2.includes('<img src="/media/system%20architecture%20overview.png" alt="Spaces 2" title="System Architecture" loading="lazy">'));
});

test('Supports parentheses in URLs (single, multiple, and balanced)', () => {
  const md1 = '![Paren 1](media/photo(1).png)';
  const html1 = Renderer.renderMarkdown(md1);
  assert.ok(html1.includes('<img src="/media/photo(1).png" alt="Paren 1" loading="lazy">'));

  const md2 = '![Paren 2](https://example.com/media/arch(final)(v2).png "Architecture Final")';
  const html2 = Renderer.renderMarkdown(md2);
  assert.ok(html2.includes('<img src="https://example.com/media/arch(final)(v2).png" alt="Paren 2" title="Architecture Final" loading="lazy">'));
});

test('Supports optional titles in double and single quotes', () => {
  const md1 = '![Title 1](media/pic.png "My Great Title")';
  const html1 = Renderer.renderMarkdown(md1);
  assert.ok(html1.includes('title="My Great Title"'));
  assert.ok(html1.includes('src="/media/pic.png"'));

  const md2 = "![Title 2](media/pic.png 'Another Great Title')";
  const html2 = Renderer.renderMarkdown(md2);
  assert.ok(html2.includes('title="Another Great Title"'));
  assert.ok(html2.includes('src="/media/pic.png"'));
});

test('Supports multiple images in the same document and paragraphs', () => {
  const md = `
## Article Overview

Here is diagram 1:
![Diagram 1](media/arch_1.png "First Diagram")

And here is diagram 2:
![Diagram 2](https://example.com/images/arch_2(final).png "Second Diagram")
`;
  const html = Renderer.renderMarkdown(md);
  assert.ok(html.includes('<img src="/media/arch_1.png" alt="Diagram 1" title="First Diagram" loading="lazy">'));
  assert.ok(html.includes('<img src="https://example.com/images/arch_2(final).png" alt="Diagram 2" title="Second Diagram" loading="lazy">'));
});

test('Rejects dangerous injection schemes in image src', () => {
  const md1 = '![XSS](javascript:alert(1))';
  const html1 = Renderer.renderMarkdown(md1);
  assert.ok(!html1.includes('javascript:'));

  const md2 = '![XSS](data:text/html,<script>alert(1)</script>)';
  const html2 = Renderer.renderMarkdown(md2);
  assert.ok(!html2.includes('data:'));
});

test('Bold and italic text around images works correctly without corrupting URLs', () => {
  const md = `
**Bold Header**

![Cover](https://supabase.co/media/my_post_cover_image.png "Cover")

*This is an italic caption with _important_ text.*
`;
  const html = Renderer.renderMarkdown(md);
  assert.ok(html.includes('<strong>Bold Header</strong>'));
  assert.ok(html.includes('<img src="https://supabase.co/media/my_post_cover_image.png" alt="Cover" title="Cover" loading="lazy">'));
  assert.ok(html.includes('<em>This is an italic caption with <em>important</em> text.</em>'));
});


console.log('\n======================================================');
console.log('3. MOBILE NAVIGATION COMPONENT & RESPONSIVE CSS TESTS');
console.log('======================================================');

test('HTML structure includes fixed mobile bottom navigation with plain text only', () => {
  const indexHtml = fs.readFileSync('frontend/index.html', 'utf8');
  assert.ok(indexHtml.includes('<nav class="mobile-bottom-nav" id="mobile-bottom-nav" aria-label="Mobile Navigation">'));
  assert.ok(indexHtml.includes('href="#hero" class="mobile-nav-link" id="mobile-nav-home">HOME</a>'));
  assert.ok(indexHtml.includes('href="#sec-blog" class="mobile-nav-link" id="mobile-nav-articles">ARTICLES</a>'));
  assert.ok(indexHtml.includes('href="#sec-projects" class="mobile-nav-link" id="mobile-nav-projects">PROJECTS</a>'));
  assert.ok(indexHtml.includes('href="#sec-contact" class="mobile-nav-link" id="mobile-nav-contact">CONTACT</a>'));

  // Ensure no emojis, icons, or graphics in mobile nav bar
  const navSectionMatch = indexHtml.match(/<nav class="mobile-bottom-nav"[\s\S]*?<\/nav>/);
  assert.ok(navSectionMatch, 'Mobile bottom nav found');
  const navHtml = navSectionMatch[0];
  assert.ok(!navHtml.includes('<img'), 'No images in mobile nav');
  assert.ok(!navHtml.includes('<svg'), 'No SVGs in mobile nav');
  assert.ok(!navHtml.includes('icon'), 'No icon classes in mobile nav');
  // Check exact 4 text contents
  const linkTexts = Array.from(navHtml.matchAll(/>([^<]+)<\/a>/g)).map(m => m[1].trim());
  assert.deepStrictEqual(linkTexts, ['HOME', 'ARTICLES', 'PROJECTS', 'CONTACT']);
});

test('Desktop sidebar navigation is preserved in index.html and global.css', () => {
  const indexHtml = fs.readFileSync('frontend/index.html', 'utf8');
  assert.ok(indexHtml.includes('<aside class="mac-sidebar" id="mac-sidebar" aria-label="Main Navigation">'));
  assert.ok(indexHtml.includes('<ul id="dynamic-nav-links" class="sidebar-menu">'));

  const globalCss = fs.readFileSync('frontend/css/global.css', 'utf8');
  assert.ok(globalCss.includes('.mac-sidebar {'));
  assert.ok(globalCss.includes('.mobile-bottom-nav {'));
  assert.ok(globalCss.includes('display: none;'));
});

test('Responsive CSS hides sidebar and displays fixed mobile bottom nav with safe-area insets', () => {
  const responsiveCss = fs.readFileSync('frontend/css/responsive.css', 'utf8');
  assert.ok(responsiveCss.includes('@media (max-width: 768px)'));
  assert.ok(responsiveCss.includes('.mac-sidebar {'));
  assert.ok(responsiveCss.includes('display: none !important;'));
  assert.ok(responsiveCss.includes('.mobile-bottom-nav {'));
  assert.ok(responsiveCss.includes('position: fixed;'));
  assert.ok(responsiveCss.includes('bottom: 0;'));
  assert.ok(responsiveCss.includes('env(safe-area-inset-bottom'));
  assert.ok(responsiveCss.includes('.mobile-nav-link {'));
  assert.ok(responsiveCss.includes('clamp('));
});

console.log('\n======================================================');
console.log(`ALL ${passedCount}/${totalCount} TESTS PASSED SUCCESSFULLY!`);
console.log('======================================================\n');
