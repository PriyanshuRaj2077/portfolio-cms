/* ==========================================================================
   CONFIG & PUBLICATION SYSTEM SETTINGS
   ========================================================================== */

/**
 * Resolves the primary published-content base URL.
 * In local dev, defaults to '/data/published/default'.
 * In production (e.g. Vercel), dynamically resolves from window configuration
 * (e.g. Cloudflare R2 public URL or Render public endpoint).
 */

 window.__PORTFOLIO_CONFIG__ = {
   PUBLIC_CONTENT_BASE_URL:
     "https://pwgndwuwzzlmpxymeoqs.supabase.co/storage/v1/object/public/portfolio/data/published/default"
 };

const getResolvedContentBaseUrl = () => {
  if (typeof window === 'undefined') return '/data/published/default';

  const customUrl = (window.__PORTFOLIO_CONFIG__ && window.__PORTFOLIO_CONFIG__.PUBLIC_CONTENT_BASE_URL)
    || window.PORTFOLIO_CONTENT_BASE_URL
    || (window.__ENV__ && window.__ENV__.PUBLIC_CONTENT_BASE_URL);

  if (customUrl && typeof customUrl === 'string' && customUrl.trim()) {
    return customUrl.trim().replace(/\/+$/, '');
  }

  return '/data/published/default';
};

const getResolvedApiBaseUrl = () => {
  if (typeof window === 'undefined') return '';

  const customUrl = (window.__PORTFOLIO_CONFIG__ && window.__PORTFOLIO_CONFIG__.API_BASE_URL)
    || window.PORTFOLIO_API_URL
    || (window.__ENV__ && window.__ENV__.API_BASE_URL);

  if (customUrl && typeof customUrl === 'string' && customUrl.trim()) {
    return customUrl.trim().replace(/\/+$/, '');
  }

  return '';
};

const CONFIG = {
  // Root-aware base URL for published static content
  PUBLIC_CONTENT_BASE_URL: getResolvedContentBaseUrl(),

  // Bundled fallback base path
  BUNDLED_FALLBACK_BASE_URL: '/data/published/default',

  // API base URL for public endpoints
  API_BASE_URL: getResolvedApiBaseUrl(),

  // Default Published Manifest File Name
  MANIFEST_FILE: 'manifest.json'
};

// Freeze config object to prevent runtime mutations
Object.freeze(CONFIG);
