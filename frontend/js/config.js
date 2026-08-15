/* ==========================================================================
   CONFIG & PUBLICATION SYSTEM SETTINGS
   ========================================================================== */

const CONFIG = {
  // Base URL for published static content (Can be updated to CDN URL in prod environment)
  PUBLIC_CONTENT_BASE_URL: './data/published/default',

  // Fallback path if CDN or primary content URL is unreachable
  BUNDLED_FALLBACK_BASE_URL: './data/published/default',

  // App Identity
  BRAND_NAME: "Priyanshu's Portfolio",
  HERO_NAME: "PRIYANSHU",

  // Default Published Manifest File Name
  MANIFEST_FILE: 'manifest.json'
};

// Freeze config object to prevent runtime mutations
Object.freeze(CONFIG);
