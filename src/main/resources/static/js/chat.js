/* Chat — Premium Fashion Assistant
 * SSE streaming, EventSource management, auto-reconnect, markdown light.
 */

(function () {
  'use strict';

  // ---- State ----
  const State = {
    activeRunId: null,
    eventSource: null,
    retryCount: 0,
    maxRetries: 3,
    accumulatedContent: '',
    streamingMsgEl: null,
    isStreaming: false,
    isSending: false,
  };

  // ---- DOM refs (cached on first call) ----
  let _els = {};

  function ensureEls() {
    if (_els.thread) return;
    _els = {
      thread: document.getElementById('chat-thread'),
      input: document.getElementById('chat-input'),
      sendBtn: document.getElementById('chat-send-btn'),
      form: document.getElementById('chat-form'),
      empty: document.getElementById('chat-empty'),
      error: document.getElementById('chat-error'),
      reconnecting: document.getElementById('chat-reconnecting'),
      retryBtn: document.getElementById('chat-retry-btn'),
      contextBadge: document.getElementById('chat-context-badge'),
      contextCard: document.getElementById('chat-context-card'),
      modelSelect: document.getElementById('chat-model-select'),
    };
  }

  // ---- Helpers ----

  function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.content : '';
  }

  function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.content : 'X-CSRF-TOKEN';
  }

  function scrollToBottom() {
    ensureEls();
    if (_els.thread) {
      _els.thread.scrollTop = _els.thread.scrollHeight;
    }
  }

  function formatTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
  }

  /** Light markdown rendering: bold, italic, lists, line breaks. */
  function renderMarkdown(text) {
    let html = escapeHtml(text);
    // Bold
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    // Italic
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    // Unordered lists
    html = html.replace(/^[\s]*[-*]\s+(.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');
    // Line breaks
    html = html.replace(/\n/g, '<br>');
    return html;
  }

  // ---- Message rendering ----

  function createMessageEl(role, content, id, createdAt) {
    const div = document.createElement('div');
    const isUser = role === 'user';
    const isPolicy = role === 'policy';
    const displayRole = isPolicy ? 'assistant' : role;

    div.className = 'chat-message chat-message-' + displayRole
      + (isPolicy ? ' chat-message-policy' : '');
    div.dataset.messageId = id || '';
    div.dataset.role = isPolicy ? 'policy' : role;

    var inner = '<div class="chat-message-role">'
      + (isUser ? 'Tú' : (isPolicy ? 'Asistente (política)' : 'Asistente'))
      + '</div>'
      + '<div class="chat-message-content">' + renderMarkdown(content) + '</div>';

    if (createdAt) {
      inner += '<div class="chat-message-time">' + formatTime(createdAt) + '</div>';
    }

    // Feedback thumbs for assistant messages
    if (!isUser && !isPolicy) {
      inner +=       '<div class="chat-feedback" data-run-id="' + (id || '') + '" data-message-id="' + (id || '') + '">'
        + '<button class="chat-feedback-btn" data-rating="up" title="Útil">👍</button>'
        + '<button class="chat-feedback-btn" data-rating="down" title="No útil">👎</button>'
        + '</div>';
    }

    div.innerHTML = inner;
    return div;
  }

  function addMessage(role, content, id, createdAt) {
    ensureEls();
    var el = createMessageEl(role, content, id, createdAt);
    _els.thread.appendChild(el);
    scrollToBottom();
    return el;
  }

  function showEmptyState() {
    ensureEls();
    if (_els.empty) _els.empty.style.display = '';
    if (_els.error) _els.error.style.display = 'none';
  }

  function hideEmptyState() {
    ensureEls();
    if (_els.empty) _els.empty.style.display = 'none';
  }

  function showError(message) {
    ensureEls();
    if (_els.error) {
      _els.error.style.display = '';
      _els.error.querySelector('p').textContent = message || 'Ocurrió un error al procesar tu mensaje.';
    }
    if (_els.empty) _els.empty.style.display = 'none';
  }

  function hideError() {
    ensureEls();
    if (_els.error) _els.error.style.display = 'none';
  }

  function showReconnecting() {
    ensureEls();
    if (_els.reconnecting) _els.reconnecting.style.display = 'flex';
  }

  function hideReconnecting() {
    ensureEls();
    if (_els.reconnecting) _els.reconnecting.style.display = 'none';
  }

  // ---- Streaming ----

  function startStreaming(runId) {
    ensureEls();
    State.activeRunId = runId;
    State.retryCount = 0;
    State.accumulatedContent = '';
    hideError();
    hideReconnecting();

    // Create a placeholder assistant message
    State.streamingMsgEl = createMessageEl('assistant', '', runId, null);
    State.streamingMsgEl.classList.add('chat-streaming');
    _els.thread.appendChild(State.streamingMsgEl);

    scrollToBottom();
    hideEmptyState();
    connectEventSource(runId);
  }

  function connectEventSource(runId) {
    if (State.eventSource) {
      State.eventSource.close();
    }

    State.isStreaming = true;
    var url = '/api/chat/stream/' + runId;
    State.eventSource = new EventSource(url);

    State.eventSource.addEventListener('chunk', function (e) {
      try {
        var data = JSON.parse(e.data);
        if (data.content) {
          State.accumulatedContent += data.content;
          updateStreamingContent();
        }
      } catch (err) {
        // ignore parse errors
      }
    });

    State.eventSource.addEventListener('done', function (e) {
      try {
        var data = JSON.parse(e.data);
        finishStreaming(data.messageId);
      } catch (err) {
        finishStreaming(null);
      }
    });

    State.eventSource.addEventListener('error', function (e) {
      try {
        var data = e.data ? JSON.parse(e.data) : null;
        var message = data ? data.content : null;
        failStreaming(message || 'Error de conexión. Inténtalo de nuevo.');
      } catch (err) {
        // If we got data but can't parse, it might be a connection issue
        State.retryCount++;
        if (State.retryCount <= State.maxRetries) {
          showReconnecting();
          // EventSource auto-reconnects, but we track retries
        } else {
          failStreaming('No se pudo conectar después de varios intentos.');
        }
      }
    });

    // EventSource will auto-reconnect on its own, but we track
    State.eventSource.onerror = function () {
      if (!State.isStreaming) return;
      State.retryCount++;
      if (State.retryCount <= State.maxRetries) {
        showReconnecting();
      } else {
        // Give up after max retries
        failStreaming('Se perdió la conexión. Recarga la página para reconectar.');
      }
    };
  }

  function updateStreamingContent() {
    if (!State.streamingMsgEl) return;
    var contentEl = State.streamingMsgEl.querySelector('.chat-message-content');
    if (contentEl) {
      contentEl.innerHTML = renderMarkdown(State.accumulatedContent);
      scrollToBottom();
    }
  }

  function finishStreaming(messageId) {
    State.isStreaming = false;
    if (State.eventSource) {
      State.eventSource.close();
      State.eventSource = null;
    }
    if (State.streamingMsgEl) {
      State.streamingMsgEl.classList.remove('chat-streaming');
      if (messageId) {
        State.streamingMsgEl.dataset.messageId = messageId;
        // Wire feedback buttons to this run
        var feedbackEl = State.streamingMsgEl.querySelector('.chat-feedback');
        if (feedbackEl) {
          feedbackEl.dataset.runId = messageId;
          feedbackEl.classList.add('visible');
        }
      }
    }
    State.activeRunId = null;
    State.accumulatedContent = '';
    State.streamingMsgEl = null;
    hideReconnecting();
    enableInput();
  }

  function failStreaming(message) {
    State.isStreaming = false;
    if (State.eventSource) {
      State.eventSource.close();
      State.eventSource = null;
    }
    // Remove the streaming placeholder
    if (State.streamingMsgEl && State.streamingMsgEl.parentNode) {
      State.streamingMsgEl.parentNode.removeChild(State.streamingMsgEl);
    }
    State.streamingMsgEl = null;
    State.activeRunId = null;
    State.accumulatedContent = '';
    hideReconnecting();
    showError(message);
    enableInput();
  }

  // ---- Send message ----

  function disableInput() {
    ensureEls();
    if (_els.input) _els.input.disabled = true;
    if (_els.sendBtn) _els.sendBtn.disabled = true;
  }

  function enableInput() {
    ensureEls();
    if (_els.input) _els.input.disabled = false;
    if (_els.sendBtn) _els.sendBtn.disabled = false;
    if (_els.input) _els.input.focus();
  }

  function sendMessage(content) {
    ensureEls();
    var sessionId = document.getElementById('chat-session-id')?.value;
    if (!sessionId) {
      showError('No hay una sesión activa. Crea una nueva conversación.');
      return;
    }
    if (!content || !content.trim() || State.isSending) return;

    var messageToSend = content.trim();
    State.isSending = true;

    disableInput();
    hideError();

    var model = _els.modelSelect ? _els.modelSelect.value : '';

    // Add user message immediately
    addMessage('user', messageToSend, null, new Date().toISOString());
    _els.input.value = '';
    _els.input.style.height = 'auto';

    // POST to create the run
    var csrfToken = getCsrfToken();
    var csrfHeader = getCsrfHeader();

    var headers = {
      'Content-Type': 'application/json',
    };
    if (csrfToken) {
      headers[csrfHeader] = csrfToken;
    }

    fetch('/api/chat/sessions/' + sessionId + '/messages', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ content: messageToSend, model: model }),
    })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        State.isSending = false;
        if (data.blocked) {
          // Policy blocked — show refusal as assistant message
          addMessage('policy', data.refusalMessage || 'No puedo procesar esa solicitud.', null, new Date().toISOString());
          enableInput();
        } else if (data.runId) {
          startStreaming(data.runId);
        } else {
          showError('Error al procesar el mensaje.');
          enableInput();
        }
      })
      .catch(function (err) {
        State.isSending = false;
        showError('Error de conexión. Verifica tu conexión e inténtalo de nuevo.');
        enableInput();
      });
  }

  // ---- Feedback ----

  function submitFeedback(runId, rating) {
    if (!runId) return;
    var feedbackEl = document.querySelector('.chat-feedback[data-run-id="' + runId + '"]');
    var messageId = feedbackEl ? feedbackEl.dataset.messageId : runId;
    var csrfToken = getCsrfToken();
    var csrfHeader = getCsrfHeader();
    var headers = { 'Content-Type': 'application/json' };
    if (csrfToken) headers[csrfHeader] = csrfToken;
    fetch('/api/chat/messages/' + messageId + '/feedback', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ rating: rating, comment: '' }),
    }).catch(function () {
      // silent fail — feedback is non-critical
    });
  }

  // ---- Context card toggle ----

  function toggleContextCard() {
    ensureEls();
    if (_els.contextCard) {
      _els.contextCard.classList.toggle('open');
    }
  }

  // ---- New session ----

  function createNewSession() {
    var csrfToken = getCsrfToken();
    var csrfHeader = getCsrfHeader();

    var headers = {
      'Content-Type': 'application/json',
    };
    if (csrfToken) {
      headers[csrfHeader] = csrfToken;
    }

    fetch('/api/chat/sessions', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ title: 'Nueva conversaci\u00f3n', model: '' }),
    })
      .then(function (r) { return r.json(); })
      .then(function (session) {
        if (session.id) {
          window.location.href = '/chat/' + session.id;
        }
      })
      .catch(function () {
        showError('No se pudo crear la conversación.');
      });
  }

  // ---- Delete session ----

  function deleteSession(sessionId) {
    if (!confirm('¿Eliminar esta conversación? Esta acción no se puede deshacer.')) return;

    var csrfToken = getCsrfToken();
    var csrfHeader = getCsrfHeader();

    var headers = {};
    if (csrfToken) {
      headers[csrfHeader] = csrfToken;
    }

    fetch('/api/chat/sessions/' + sessionId, {
      method: 'DELETE',
      headers: headers,
    })
      .then(function (r) {
        if (r.ok) {
          window.location.href = '/chat';
        }
      })
      .catch(function () {
        showError('No se pudo eliminar la conversación.');
      });
  }

  // ---- Session management (title update) ----

  function updateSessionTitle(sessionId, title) {
    var csrfToken = getCsrfToken();
    var csrfHeader = getCsrfHeader();

    var headers = {
      'Content-Type': 'application/json',
    };
    if (csrfToken) {
      headers[csrfHeader] = csrfToken;
    }

    fetch('/api/chat/sessions/' + sessionId + '/title', {
      method: 'PATCH',
      headers: headers,
      body: JSON.stringify({ title: title }),
    }).catch(function () {
      // silent
    });
  }

  // ---- Boot ----

  function init() {
    ensureEls();

    // Form submit
    if (_els.form) {
      _els.form.addEventListener('submit', function (e) {
        e.preventDefault();
        if (State.isStreaming || State.isSending) return;
        var content = _els.input ? _els.input.value : '';
        sendMessage(content);
      });
    }

    // Input auto-resize
    if (_els.input) {
      _els.input.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';
      });
      // Enter to send (Shift+Enter for new line)
      _els.input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          if (_els.form) _els.form.dispatchEvent(new Event('submit'));
        }
      });
    }

    // Context badge toggle
    if (_els.contextBadge) {
      _els.contextBadge.addEventListener('click', toggleContextCard);
    }

    // Feedback delegation
    if (_els.thread) {
      _els.thread.addEventListener('click', function (e) {
        var btn = e.target.closest('.chat-feedback-btn');
        if (!btn) return;
        var feedback = btn.closest('.chat-feedback');
        if (!feedback) return;
        var runId = feedback.dataset.runId;
        var rating = btn.dataset.rating;
        if (runId && rating) {
          // Toggle active state
          var siblings = feedback.querySelectorAll('.chat-feedback-btn');
          siblings.forEach(function (s) { s.classList.remove('active'); });
          btn.classList.add('active');
          submitFeedback(runId, rating);
        }
      });
    }

    document.addEventListener('click', function (e) {
      var newSessionButton = e.target.closest('[data-chat-new-session]');
      if (newSessionButton) {
        e.preventDefault();
        createNewSession();
        return;
      }

      var promptButton = e.target.closest('[data-chat-prompt]');
      if (promptButton) {
        e.preventDefault();
        ensureEls();
        if (_els.input) {
          _els.input.value = promptButton.dataset.chatPrompt || promptButton.textContent.trim();
          _els.input.dispatchEvent(new Event('input'));
          _els.input.focus();
        }
      }
    });

    // Retry button
    if (_els.retryBtn) {
      _els.retryBtn.addEventListener('click', function () {
        hideError();
        // Re-send the last message if there was a streaming failure
        if (_els.input && _els.input.value.trim()) {
          sendMessage(_els.input.value);
        }
      });
    }

    // Close context card on Escape
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && _els.contextCard && _els.contextCard.classList.contains('open')) {
        _els.contextCard.classList.remove('open');
      }
    });

    // Auto-scroll on page load if messages exist
    setTimeout(scrollToBottom, 100);
  }

  // Expose for HTMX / inline use
  window.Chat = {
    init: init,
    sendMessage: sendMessage,
    createNewSession: createNewSession,
    deleteSession: deleteSession,
    toggleContextCard: toggleContextCard,
  };

  // Auto-init on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
