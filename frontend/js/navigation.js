/* ==========================================================================
   NAVIGATION & MAC-STYLE SIDEBAR CONTROLLER
   Desktop Sidebar Rail & Mobile Bottom Bar
   ========================================================================== */

const Navigation = {
  init() {
    this.setupSmoothScroll();
    this.setupScrollSpy();
    this.setupMobileTapReveal();
  },

  setupSmoothScroll() {
    document.addEventListener('click', (e) => {
      const anchor = e.target.closest('a[href^="#"]');
      if (!anchor) return;

      const targetId = anchor.getAttribute('href');
      if (!targetId || targetId === '#') return;

      // If currently viewing an article page, switch back to portfolio view
      const articleView = document.getElementById('article-view');
      const portfolioView = document.getElementById('portfolio-view');
      if (articleView && articleView.style.display === 'block' && portfolioView) {
        articleView.style.display = 'none';
        portfolioView.style.display = 'block';
        if (window.location.pathname.startsWith('/blog/') || window.location.hash.startsWith('#blog/')) {
          try {
            history.pushState(null, '', '/');
          } catch (err) {
            window.location.hash = '';
          }
        }
      }

      if (targetId === '#hero') {
        e.preventDefault();
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
        return;
      }

      let targetElem = document.querySelector(targetId);
      if (!targetElem) {
        if (targetId === '#sec-blog' || targetId.includes('blog') || targetId.includes('article')) {
          targetElem = document.querySelector('section[id*="blog"]') || document.querySelector('.blogs-editorial-list');
        } else if (targetId === '#sec-projects' || targetId.includes('project')) {
          targetElem = document.querySelector('section[id*="project"]') || document.querySelector('.projects-editorial-list');
        } else if (targetId === '#sec-contact' || targetId.includes('contact')) {
          targetElem = document.querySelector('section[id*="contact"]') || document.querySelector('.contact-editorial');
        }
      }

      if (targetElem) {
        e.preventDefault();
        const headerOffset = 24;
        const elementPosition = targetElem.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.pageYOffset - headerOffset;

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        });
      }
    });
  },

  setupScrollSpy() {
    const sections = document.querySelectorAll('section[id]');
    const desktopLinks = document.querySelectorAll('.sidebar-link');
    const mobileLinks = document.querySelectorAll('.mobile-nav-link');

    if (!sections.length && !desktopLinks.length && !mobileLinks.length) return;

    window.addEventListener('scroll', () => {
      let currentSectionId = '';
      const scrollPosition = window.pageYOffset + 140;

      if (window.pageYOffset < 150) {
        currentSectionId = 'hero';
      } else {
        sections.forEach(section => {
          const sectionTop = section.offsetTop;
          const sectionHeight = section.offsetHeight;

          if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
            currentSectionId = section.id;
          }
        });
      }

      // Update desktop sidebar links
      desktopLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${currentSectionId}`) {
          link.classList.add('active');
        }
      });

      // Update mobile bottom nav links (HOME · ARTICLES · PROJECTS · CONTACT)
      mobileLinks.forEach(link => {
        link.classList.remove('active');
        const href = link.getAttribute('href');
        if (href === `#${currentSectionId}`) {
          link.classList.add('active');
        } else if (href === '#sec-blog' && currentSectionId.includes('blog')) {
          link.classList.add('active');
        } else if (href === '#sec-projects' && currentSectionId.includes('project')) {
          link.classList.add('active');
        } else if (href === '#sec-contact' && currentSectionId.includes('contact')) {
          link.classList.add('active');
        }
      });
    }, { passive: true });
  },

  setupMobileTapReveal() {
    const sidebar = document.getElementById('mac-sidebar');
    if (!sidebar) return;

    // Support mobile tap reveal brief behavior
    sidebar.addEventListener('touchstart', (e) => {
      const link = e.target.closest('.sidebar-link');
      if (link) {
        link.classList.add('active');
        setTimeout(() => {
          link.classList.remove('active');
        }, 1200);
      }
    }, { passive: true });
  }
};
