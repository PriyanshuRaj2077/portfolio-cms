/* ==========================================================================
   CONFIG & PUBLICATION SYSTEM SETTINGS
   ========================================================================== */

const CONFIG = {
  // Root-aware base URL for published static content
  PUBLIC_CONTENT_BASE_URL: '/data/published/default',

  // Bundled fallback base path
  BUNDLED_FALLBACK_BASE_URL: '/data/published/default',

  // Default Published Manifest File Name
  MANIFEST_FILE: 'manifest.json'
};

// Freeze config object to prevent runtime mutations
Object.freeze(CONFIG);

