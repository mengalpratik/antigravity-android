// web/src/patches/matrix_core.js
// MATRIX CORE - Antigravity Android Native Control Center
// Matrix Core settings screen is natively rendered by React inside the Settings stack.
// This companion patch purges legacy header badges and exposes runtime utilities.

(function () {
  'use strict';

  var OLD_ROOT_BADGE_ID = 'agy-root-indicator-badge';
  var OLD_ROOT_MODAL_ID = 'agy-root-diagnostic-modal';

  function purgeLegacyBadge() {
    var b = document.getElementById(OLD_ROOT_BADGE_ID);
    if (b) b.remove();
    var m = document.getElementById(OLD_ROOT_MODAL_ID);
    if (m) m.remove();
  }

  function getRootStatus() {
    var active = false;
    var available = false;
    var provider = 'Unknown';
    var suPath = '/system/bin/su';

    try {
      if (window.__APP_CONFIG__) {
        active = Boolean(window.__APP_CONFIG__.rootActive);
        available = Boolean(window.__APP_CONFIG__.rootAvailable);
        provider = window.__APP_CONFIG__.rootProvider || 'Unknown';
      }
      if (window.Android && typeof window.Android.isRootActive === 'function') {
        active = window.Android.isRootActive();
      }
      if (window.Android && typeof window.Android.isRootAvailable === 'function') {
        available = window.Android.isRootAvailable();
      }
      if (window.Android && typeof window.Android.getRootProvider === 'function') {
        var p = window.Android.getRootProvider();
        if (p && p !== 'None') provider = p;
      }
      if (window.Android && typeof window.Android.getRootStatusJson === 'function') {
        var info = JSON.parse(window.Android.getRootStatusJson());
        if (info.suPath) suPath = info.suPath;
      }
    } catch (e) {
      console.warn('[MatrixCore] Error reading root status:', e);
    }

    return {
      active: active,
      available: available,
      provider: provider,
      suPath: suPath
    };
  }

  function setupObserver() {
    purgeLegacyBadge();
    if (window.MutationObserver && document.body) {
      var observer = new MutationObserver(purgeLegacyBadge);
      observer.observe(document.body, { childList: true, subtree: true });
    }
  }

  window.MatrixCore = {
    getRootStatus: getRootStatus,
    openSettings: function () {
      if (typeof window.openSettings === 'function') {
        window.openSettings();
      }
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupObserver);
  } else {
    setupObserver();
  }
})();

