/* ==========================================================================
   NAVIGATION & MAC-STYLE SIDEBAR CONTROLLER
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
      if (targetId === '#') return;

      const targetElem = document.querySelector(targetId);
      if (targetElem) {
        e.preventDefault();
        const headerOffset = window.innerWidth <= 768 ? 64 : 40;
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
    const navLinks = document.querySelectorAll('.sidebar-link');

    if (!sections.length || !navLinks.length) return;

    window.addEventListener('scroll', () => {
      let currentSectionId = '';
      const scrollPosition = window.pageYOffset + 140;

      sections.forEach(section => {
        const sectionTop = section.offsetTop;
        const sectionHeight = section.offsetHeight;

        if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
          currentSectionId = section.id;
        }
      });

      navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${currentSectionId}`) {
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
