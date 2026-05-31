window.CompanionPositioning = (function () {
  'use strict';

  function viewportSize() {
    const vv = window.visualViewport;
    if (vv) {
      return { width: vv.width, height: vv.height };
    }
    return { width: window.innerWidth, height: window.innerHeight };
  }

  function setPosition(root, left, top) {
    const vp = viewportSize();
    const width = root.offsetWidth || 72;
    const height = root.offsetHeight || 72;
    const maxLeft = vp.width - width - 8;
    const maxTop = vp.height - height - 8;
    const safeLeft = Math.max(8, Math.min(left, maxLeft));
    const safeTop = Math.max(8, Math.min(top, maxTop));

    root.style.left = safeLeft + 'px';
    root.style.top = safeTop + 'px';
    root.style.right = 'auto';
    root.style.bottom = 'auto';
  }

  function ensureRootInViewport(root) {
    const rect = root.getBoundingClientRect();
    setPosition(root, rect.left, rect.top);
  }

  function clearPanelPosition(panel) {
    panel.style.left = '';
    panel.style.top = '';
    panel.style.right = '';
    panel.style.bottom = '';
    panel.style.maxHeight = '';
  }

  function currentAnchorRect(root) {
    const rect = root.getBoundingClientRect();
    return {
      top: rect.top,
      bottom: rect.bottom,
      left: rect.left,
      right: rect.right,
      width: rect.width,
      height: rect.height
    };
  }

  function positionPanelForViewport(root, panel, anchorRect) {
    if (!panel) return;

    if (window.matchMedia('(max-width: 599px)').matches) {
      clearPanelPosition(panel);
      return;
    }

    const vp = viewportSize();
    const anchor = anchorRect || currentAnchorRect(root);
    const topBar = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--top-bar-h')) || 60;
    const topInset = topBar + 12;
    const bottomInset = 16;
    const sideInset = 8;
    const gap = 16;
    const availableAbove = Math.max(180, anchor.top - topInset);
    const availableBelow = Math.max(180, vp.height - anchor.bottom - bottomInset);
    const openBelow = availableAbove < 280 && availableBelow > availableAbove;
    const openRight = anchor.left < (vp.width / 2);

    panel.style.left = '';
    panel.style.top = '';
    panel.style.right = '';
    panel.style.bottom = '';

    const panelWidth = panel.offsetWidth || 390;
    let left = openRight ? anchor.left : anchor.right - panelWidth;
    left = Math.max(sideInset, Math.min(left, vp.width - panelWidth - sideInset));
    panel.style.left = Math.round(left) + 'px';

    if (openBelow) {
      panel.style.top = Math.round(anchor.bottom + gap) + 'px';
      panel.style.maxHeight = Math.floor(Math.min(680, availableBelow)) + 'px';
      return;
    }

    panel.style.bottom = Math.round(vp.height - anchor.top + gap) + 'px';
    panel.style.maxHeight = Math.floor(Math.min(680, availableAbove)) + 'px';
  }

  function startDrag(root, trigger, state, savePosition) {
    return function (event) {
      if (state.isOpen || event.button !== 0) return;

      const startX = event.clientX;
      const startY = event.clientY;
      const rect = root.getBoundingClientRect();
      const initialLeft = rect.left;
      const initialTop = rect.top;
      state.dragMoved = false;
      trigger.setPointerCapture(event.pointerId);

      function onMove(moveEvent) {
        const dx = moveEvent.clientX - startX;
        const dy = moveEvent.clientY - startY;
        if (Math.abs(dx) + Math.abs(dy) > 5) state.dragMoved = true;
        setPosition(root, initialLeft + dx, initialTop + dy);
      }

      function onUp() {
        trigger.removeEventListener('pointermove', onMove);
        trigger.removeEventListener('pointerup', onUp);
        savePosition();
      }

      trigger.addEventListener('pointermove', onMove);
      trigger.addEventListener('pointerup', onUp);
    };
  }

  return {
    setPosition: setPosition,
    ensureRootInViewport: ensureRootInViewport,
    clearPanelPosition: clearPanelPosition,
    currentAnchorRect: currentAnchorRect,
    positionPanelForViewport: positionPanelForViewport,
    startDrag: startDrag
  };
})();
