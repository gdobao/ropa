/* Global companion assistant — swappable shell + skin. */
(function () {
  'use strict';

  const state = window.CompanionState.createState();
  const STORAGE_KEY = window.CompanionState.STORAGE_KEY;
  const DEFAULT_TITLE = window.CompanionState.DEFAULT_TITLE;
  const STREAM_TIMEOUT = window.CompanionState.STREAM_TIMEOUT;

  let root;
  let els = {};

  function rebindDom() {
    root = document.querySelector('[data-companion-root]');
    if (!root) return;
    els = window.CompanionDom.query(root, document);
  }

  function savePosition() {
    const rect = root.getBoundingClientRect();
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ left: rect.left, top: rect.top }));
  }

  function restorePosition() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (saved && Number.isFinite(saved.left) && Number.isFinite(saved.top)) {
        window.CompanionPositioning.setPosition(root, saved.left, saved.top);
      }
    } catch (ignored) {
      // ignore corrupted localStorage
    }
  }

  function setStatus(message) {
    window.CompanionDom.setStatus(els, message);
  }

  function setInputDisabled(disabled) {
    els.input.disabled = disabled;
    els.send.disabled = disabled;
  }

  function scrollThread() {
    window.CompanionDom.scrollThread(els);
  }

  function addMessage(role, content) {
    return window.CompanionDom.addMessage(els, role, content);
  }

  function loadContext() {
    window.CompanionApi.readContext()
      .then(function (data) {
        if (!data) return;
        const tip = Array.isArray(data.tips) && data.tips.length > 0 ? data.tips[0] : data.summary;
        if (tip) {
          els.bubbleText.textContent = tip;
          root.classList.add('has-tip');
        }
        if (data.summary) {
          els.context.textContent = data.summary;
        }
      })
      .catch(function () {
        els.context.textContent = 'No pude leer el contexto del armario todavía.';
      });
  }

  function ensureSession() {
    if (state.sessionId) return Promise.resolve(state.sessionId);

    return window.CompanionApi.listSessions().then(function (sessions) {
      if (Array.isArray(sessions) && sessions.length > 0) {
        state.sessionId = sessions[0].id;
        return state.sessionId;
      }
      return window.CompanionApi.createSession(DEFAULT_TITLE).then(function (session) {
        state.sessionId = session.id;
        return state.sessionId;
      });
    });
  }

  function loadMessages() {
    if (!state.sessionId) return Promise.resolve();
    return window.CompanionApi.listMessages(state.sessionId).then(function (messages) {
      if (!Array.isArray(messages) || messages.length === 0) return;
      els.thread.innerHTML = '';
      messages.forEach(function (message) {
        addMessage(message.role === 'user' ? 'user' : 'assistant', message.content || '');
      });
    });
  }

  function openPanel() {
    state.anchorRect = window.CompanionPositioning.currentAnchorRect(root);
    state.isOpen = true;
    state.previouslyFocused = document.activeElement;
    root.classList.add('is-open');
    els.panel.hidden = false;
    window.CompanionPositioning.positionPanelForViewport(root, els.panel, state.anchorRect);
    els.trigger.setAttribute('aria-expanded', 'true');
    setStatus('Ayudante de estilo abierto.');
    loadContext();

    ensureSession().then(loadMessages).finally(function () {
      window.CompanionPositioning.positionPanelForViewport(root, els.panel, state.anchorRect);
      setTimeout(function () { els.input.focus(); }, 0);
    });
  }

  function closePanel() {
    state.isOpen = false;
    root.classList.remove('is-open');
    state.anchorRect = null;
    window.CompanionPositioning.clearPanelPosition(els.panel);
    els.panel.hidden = true;
    els.trigger.setAttribute('aria-expanded', 'false');
    setStatus('Ayudante de estilo cerrado.');

    if (state.previouslyFocused && typeof state.previouslyFocused.focus === 'function') {
      state.previouslyFocused.focus();
      return;
    }
    els.trigger.focus();
  }

  function repositionOpenPanel() {
    window.CompanionPositioning.ensureRootInViewport(root);
    if (!state.isOpen) return;
    state.anchorRect = window.CompanionPositioning.currentAnchorRect(root);
    window.CompanionPositioning.positionPanelForViewport(root, els.panel, state.anchorRect);
  }

  function moveBy(dx, dy) {
    const rect = root.getBoundingClientRect();
    window.CompanionPositioning.setPosition(root, rect.left + dx, rect.top + dy);
    savePosition();
    setStatus('Posición del ayudante actualizada.');
  }

  function handleTriggerKeydown(event) {
    if (!event.shiftKey || !event.key.startsWith('Arrow')) return;
    event.preventDefault();
    const step = event.altKey ? 40 : 16;
    if (event.key === 'ArrowLeft') moveBy(-step, 0);
    if (event.key === 'ArrowRight') moveBy(step, 0);
    if (event.key === 'ArrowUp') moveBy(0, -step);
    if (event.key === 'ArrowDown') moveBy(0, step);
  }

  function resetPosition() {
    localStorage.removeItem(STORAGE_KEY);
    root.removeAttribute('style');
    repositionOpenPanel();
    setStatus('Posición del ayudante restablecida.');
    els.input.focus();
  }

  function resetConversation() {
    if (state.isResetting) return;
    state.isResetting = true;
    els.newChat.disabled = true;

    const sessionId = state.sessionId;
    state.sessionId = null;
    state.retryCount = 0;
    els.thread.innerHTML = '';

    const done = function () {
      state.isResetting = false;
      els.newChat.disabled = false;
      addMessage('assistant', '¡Hola! Soy Colorín. Cuéntame qué necesitas y le damos una vuelta a tu armario.');
      setStatus('Conversación reiniciada.');
    };

    if (!sessionId) {
      done();
      return;
    }

    window.CompanionApi.deleteSession(sessionId)
      .catch(function () { return null; })
      .then(function () { return window.CompanionApi.createSession(DEFAULT_TITLE); })
      .then(function (session) {
        state.sessionId = session.id;
        done();
      })
      .catch(function () { done(); });
  }

  function closeStream() {
    if (state.eventSource) {
      state.eventSource.close();
      state.eventSource = null;
    }
  }

  function finishStream() {
    state.streamResolved = true;
    clearTimeout(state.streamTimeout);
    state.streamTimeout = null;
    closeStream();
    state.isSending = false;

    if (state.streamingEl) {
      state.streamingEl.classList.remove('companion-streaming');
    }

    state.streamingEl = null;
    state.accumulated = '';
    setInputDisabled(false);
    setStatus('Respuesta del ayudante completada.');
  }

  function startStream(runId) {
    closeStream();
    state.accumulated = '';
    state.streamResolved = false;
    state.retryCount = 0;
    state.streamingEl = addMessage('assistant', '');
    state.streamingEl.classList.add('companion-streaming');
    setStatus('El ayudante está respondiendo.');

    state.streamTimeout = setTimeout(function () {
      if (state.streamResolved) return;
      addMessage('error', 'El ayudante tardó demasiado en responder.');
      finishStream();
    }, STREAM_TIMEOUT);

    state.eventSource = new EventSource('/api/companion/stream/' + runId);

    state.eventSource.addEventListener('chunk', function (event) {
      try {
        const data = JSON.parse(event.data);
        if (data.content) {
          state.accumulated += data.content;
          state.streamingEl.querySelector('.companion-message-content').innerHTML = window.CompanionDom.renderMarkdown(state.accumulated);
          scrollThread();
        }
      } catch (ignored) {
        // ignore malformed stream chunk
      }
    });

    state.eventSource.addEventListener('done', function () {
      finishStream();
    });

    state.eventSource.addEventListener('stream-error', function (event) {
      try {
        const data = JSON.parse(event.data);
        addMessage('error', data.error || 'Ocurrió un error al procesar la respuesta.');
      } catch (ignored) {
        addMessage('error', 'Ocurrió un error al procesar la respuesta.');
      }
      finishStream();
    });

    state.eventSource.addEventListener('error', function () {
      if (state.streamResolved) return;
      state.retryCount = (state.retryCount || 0) + 1;
      if (state.retryCount > 3) {
        if (!state.isSending) return;
        addMessage('error', 'Se perdió la conexión con el ayudante. Intentá de nuevo.');
        finishStream();
      }
    });
  }

  function submitMessage(event) {
    event.preventDefault();
    if (state.isSending) return;

    const content = els.input.value.trim();
    if (!content) return;

    state.isSending = true;
    setInputDisabled(true);
    addMessage('user', content);
    els.input.value = '';
    window.CompanionDom.resizeInput(els.input);

    ensureSession()
      .then(function (sessionId) {
        return window.CompanionApi.sendMessage(sessionId, content);
      })
      .then(function (data) {
        if (data.blocked) {
          addMessage('assistant', data.refusalMessage || 'No puedo procesar ese pedido.');
          state.isSending = false;
          setInputDisabled(false);
          return;
        }
        if (!data.runId) throw new Error('Missing runId');
        startStream(data.runId);
      })
      .catch(function (err) {
        addMessage('error', err.message || 'No pude enviar el mensaje. Probá de nuevo.');
        state.isSending = false;
        setInputDisabled(false);
      });
  }

  function onPanelKeydown(event) {
    if (event.key === 'Escape') {
      event.preventDefault();
      closePanel();
      return;
    }
    if (event.key === 'Enter' && !event.shiftKey && event.target === els.input) {
      event.preventDefault();
      els.form.dispatchEvent(new Event('submit', { cancelable: true }));
    }
  }

  function bindEvents() {
    els.trigger.addEventListener('click', function () {
      if (state.dragMoved) {
        state.dragMoved = false;
        return;
      }
      if (state.isOpen) {
        closePanel();
      } else {
        openPanel();
      }
    });

    els.trigger.addEventListener('keydown', handleTriggerKeydown);
    els.trigger.addEventListener('pointerdown', window.CompanionPositioning.startDrag(root, els.trigger, state, savePosition));
    els.close.addEventListener('click', closePanel);
    els.newChat.addEventListener('click', resetConversation);
    els.reset.addEventListener('click', resetPosition);
    els.panel.addEventListener('keydown', onPanelKeydown);
    els.form.addEventListener('submit', submitMessage);
    els.input.addEventListener('input', function () {
      window.CompanionDom.resizeInput(els.input);
    });
    window.addEventListener('resize', repositionOpenPanel);

    if (window.visualViewport) {
      window.visualViewport.addEventListener('resize', repositionOpenPanel);
      window.visualViewport.addEventListener('scroll', repositionOpenPanel);
    }
  }

  function init() {
    rebindDom();
    if (!root || root.dataset.initialized === 'true') return;
    if (!els.trigger || !els.panel || !els.close || !els.newChat || !els.reset || !els.context || !els.thread || !els.form || !els.input || !els.send || !els.status || !els.bubbleText) {
      return;
    }
    root.dataset.initialized = 'true';

    restorePosition();
    bindEvents();
    loadContext();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  document.addEventListener('htmx:afterSettle', function () {
    if (root && root.dataset.initialized === 'true') {
      rebindDom();
      return;
    }
    init();
  });

  window.addEventListener('beforeunload', closeStream);
})();
