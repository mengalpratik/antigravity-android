// web/src/patches/root_indicator.js
// Native Root Status Indicator badge & diagnostic overlay for Antigravity Web UI.

(function () {
  'use strict';

  var ROOT_INDICATOR_ID = 'agy-root-indicator-badge';
  var ROOT_MODAL_ID = 'agy-root-diagnostic-modal';

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
      console.warn('[RootIndicator] Error reading root status:', e);
    }

    return {
      active: active,
      available: available,
      provider: provider,
      suPath: suPath
    };
  }

  function triggerOpenSettings() {
    try {
      var mobileBtn = document.querySelector('[data-testid="mobile-open-settings"], [data-testid="settings-button"]');
      if (mobileBtn) {
        mobileBtn.click();
        return;
      }
    } catch (_) {}

    try {
      var ev = new KeyboardEvent('keydown', {
        key: ',',
        code: 'Comma',
        keyCode: 188,
        which: 188,
        ctrlKey: true,
        metaKey: true,
        bubbles: true,
        cancelable: true
      });
      window.dispatchEvent(ev);
      document.dispatchEvent(ev);
    } catch (_) {}

    try {
      var search = new URLSearchParams(window.location.search);
      if (!search.get('settingsOpen')) {
        search.set('settingsOpen', 'true');
        var newUrl = window.location.pathname + '?' + search.toString();
        window.history.pushState({}, '', newUrl);
        window.dispatchEvent(new PopStateEvent('popstate'));
      }
    } catch (_) {}
  }

  // Expose globally for console or scripting
  window.openSettings = triggerOpenSettings;

  function createBadge(status) {
    var existing = document.getElementById(ROOT_INDICATOR_ID);
    if (existing) {
      existing.remove();
    }

    var badge = document.createElement('div');
    badge.id = ROOT_INDICATOR_ID;

    var isActive = status.active;
    var dotColor = isActive ? '#10b981' : '#71717a';
    var bg = isActive ? 'rgba(16, 185, 129, 0.12)' : 'rgba(113, 113, 122, 0.12)';
    var border = isActive ? '1px solid rgba(16, 185, 129, 0.35)' : '1px solid rgba(113, 113, 122, 0.25)';
    var textColor = isActive ? '#34d399' : '#a1a1aa';

    // Center badge in header so it never covers right-aligned header buttons (Settings, Refresh, Account)
    badge.style.cssText = [
      'position: fixed',
      'top: 7px',
      'left: 50%',
      'transform: translateX(-50%)',
      'z-index: 100',
      'display: flex',
      'align-items: center',
      'gap: 6px',
      'padding: 3px 8px',
      'border-radius: 9999px',
      'background: ' + bg,
      'border: ' + border,
      'color: ' + textColor,
      'font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace',
      'font-size: 11px',
      'font-weight: 600',
      'letter-spacing: 0.04em',
      'backdrop-filter: blur(8px)',
      '-webkit-backdrop-filter: blur(8px)',
      'box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25)',
      'user-select: none',
      '-webkit-tap-highlight-color: transparent',
      'transition: transform 0.15s ease, background 0.15s ease'
    ].join(';');

    // Root status section (clickable -> opens root diagnostics)
    var rootSection = document.createElement('div');
    rootSection.setAttribute('role', 'button');
    rootSection.setAttribute('tabindex', '0');
    rootSection.title = status.active
      ? 'Root Access Active (' + status.provider + ') - Click for details'
      : 'Root Inactive - Click for details';
    rootSection.style.cssText = 'display:flex; align-items:center; gap:5px; cursor:pointer;';

    var dot = document.createElement('span');
    dot.style.cssText = [
      'width: 6px',
      'height: 6px',
      'border-radius: 50%',
      'background-color: ' + dotColor,
      'box-shadow: 0 0 6px ' + dotColor,
      'display: inline-block'
    ].join(';');

    var text = document.createElement('span');
    text.textContent = isActive ? '# ROOT' : '# NO ROOT';

    rootSection.appendChild(dot);
    rootSection.appendChild(text);
    rootSection.addEventListener('click', function (e) {
      e.stopPropagation();
      showModal(getRootStatus());
    });

    // Divider
    var divider = document.createElement('span');
    divider.style.cssText = 'width:1px; height:10px; background:rgba(255,255,255,0.18); display:inline-block; margin:0 1px;';

    // Settings shortcut button (gear icon)
    var settingsBtn = document.createElement('div');
    settingsBtn.setAttribute('role', 'button');
    settingsBtn.setAttribute('tabindex', '0');
    settingsBtn.title = 'Open Settings (Ctrl+,)';
    settingsBtn.innerHTML = '⚙';
    settingsBtn.style.cssText = 'cursor:pointer; font-size:12px; line-height:1; opacity:0.85; padding:1px 2px; transition:opacity 0.15s ease;';
    settingsBtn.addEventListener('mouseenter', function () { settingsBtn.style.opacity = '1'; });
    settingsBtn.addEventListener('mouseleave', function () { settingsBtn.style.opacity = '0.85'; });
    settingsBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      triggerOpenSettings();
    });

    badge.appendChild(rootSection);
    badge.appendChild(divider);
    badge.appendChild(settingsBtn);

    document.body.appendChild(badge);
  }

  function showModal(status) {
    var old = document.getElementById(ROOT_MODAL_ID);
    if (old) old.remove();

    var overlay = document.createElement('div');
    overlay.id = ROOT_MODAL_ID;
    overlay.style.cssText = [
      'position: fixed',
      'top: 0',
      'left: 0',
      'width: 100vw',
      'height: 100vh',
      'background: rgba(0, 0, 0, 0.65)',
      'backdrop-filter: blur(4px)',
      '-webkit-backdrop-filter: blur(4px)',
      'z-index: 100000',
      'display: flex',
      'align-items: center',
      'justify-content: center',
      'padding: 16px',
      'box-sizing: border-box',
      'animation: agyFadeIn 0.15s ease'
    ].join(';');

    var card = document.createElement('div');
    card.style.cssText = [
      'background: #121826',
      'border: 1px solid rgba(255, 255, 255, 0.1)',
      'border-radius: 14px',
      'padding: 20px',
      'max-width: 360px',
      'width: 100%',
      'color: #f1f5f9',
      'font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      'box-shadow: 0 16px 36px rgba(0, 0, 0, 0.5)',
      'box-sizing: border-box'
    ].join(';');

    var header = document.createElement('div');
    header.style.cssText = 'display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;';
    
    var title = document.createElement('div');
    title.style.cssText = 'font-weight:700; font-size:15px; display:flex; align-items:center; gap:8px;';
    title.innerHTML = '<span style="color:' + (status.active ? '#10b981' : '#71717a') + '; font-size:18px;">🛡️</span> Root Privilege Status';

    var closeBtn = document.createElement('button');
    closeBtn.textContent = '✕';
    closeBtn.style.cssText = 'background:transparent; border:none; color:#94a3b8; font-size:16px; cursor:pointer; padding:4px 8px;';
    closeBtn.addEventListener('click', function () { overlay.remove(); });

    header.appendChild(title);
    header.appendChild(closeBtn);
    card.appendChild(header);

    var rows = [
      { label: 'Status', value: status.active ? 'ACTIVE (uid=0 root)' : 'INACTIVE / NOT DETECTED', color: status.active ? '#34d399' : '#f87171' },
      { label: 'Provider', value: status.provider, color: '#38bdf8' },
      { label: 'Binary Path', value: status.suPath, color: '#94a3b8' },
      { label: 'Elevation', value: status.active ? 'Direct KernelSU / Magisk su' : 'None / Shizuku Fallback', color: '#cbd5e1' }
    ];

    rows.forEach(function (r) {
      var row = document.createElement('div');
      row.style.cssText = 'display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid rgba(255,255,255,0.06); font-size:12px;';
      
      var lbl = document.createElement('span');
      lbl.style.color = '#94a3b8';
      lbl.textContent = r.label;

      var val = document.createElement('span');
      val.style.fontWeight = '600';
      val.style.color = r.color;
      val.textContent = r.value;

      row.appendChild(lbl);
      row.appendChild(val);
      card.appendChild(row);
    });

    var testBtn = document.createElement('button');
    testBtn.textContent = '⚡ Verify Root Access';
    testBtn.style.cssText = [
      'width: 100%',
      'margin-top: 16px',
      'padding: 10px',
      'border-radius: 8px',
      'border: 1px solid rgba(56, 189, 248, 0.4)',
      'background: rgba(56, 189, 248, 0.12)',
      'color: #38bdf8',
      'font-weight: 600',
      'font-size: 12px',
      'cursor: pointer',
      'transition: all 0.2s'
    ].join(';');

    var outputArea = document.createElement('pre');
    outputArea.style.cssText = [
      'display: none',
      'margin-top: 10px',
      'padding: 8px',
      'border-radius: 6px',
      'background: rgba(0, 0, 0, 0.4)',
      'color: #34d399',
      'font-size: 11px',
      'max-height: 100px',
      'overflow: auto',
      'white-space: pre-wrap',
      'word-break: break-all'
    ].join(';');

    testBtn.addEventListener('click', function () {
      testBtn.textContent = 'Verifying...';
      testBtn.disabled = true;
      try {
        if (window.Android && typeof window.Android.isRootActive === 'function') {
          var ok = window.Android.isRootActive();
          outputArea.style.display = 'block';
          outputArea.style.color = ok ? '#34d399' : '#f87171';
          outputArea.textContent = ok
            ? 'Success: uid=0(root) verified active via su!'
            : 'Error: Root not granted or su execution failed.';
        } else {
          outputArea.style.display = 'block';
          outputArea.style.color = '#f87171';
          outputArea.textContent = 'Notice: Android bridge unavailable in current frame.';
        }
      } catch (err) {
        outputArea.style.display = 'block';
        outputArea.style.color = '#f87171';
        outputArea.textContent = 'Exception: ' + (err.message || err);
      } finally {
        testBtn.textContent = '⚡ Verify Root Access';
        testBtn.disabled = false;
        createBadge(getRootStatus());
      }
    });

    var openSettingsModalBtn = document.createElement('button');
    openSettingsModalBtn.innerHTML = '⚙️ Open App Settings';
    openSettingsModalBtn.style.cssText = [
      'width: 100%',
      'margin-top: 10px',
      'padding: 10px',
      'border-radius: 8px',
      'border: 1px solid rgba(255, 255, 255, 0.15)',
      'background: rgba(255, 255, 255, 0.06)',
      'color: #f1f5f9',
      'font-weight: 600',
      'font-size: 12px',
      'cursor: pointer',
      'transition: all 0.2s'
    ].join(';');
    openSettingsModalBtn.addEventListener('click', function () {
      overlay.remove();
      triggerOpenSettings();
    });

    card.appendChild(testBtn);
    card.appendChild(outputArea);
    card.appendChild(openSettingsModalBtn);
    overlay.appendChild(card);

    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) overlay.remove();
    });

    document.body.appendChild(overlay);
  }

  function init() {
    createBadge(getRootStatus());
    // Periodically re-check root status every 15s (lightweight cached check)
    setInterval(function () {
      createBadge(getRootStatus());
    }, 15000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

