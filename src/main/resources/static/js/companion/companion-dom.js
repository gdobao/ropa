window.CompanionDom = (function () {
  'use strict';

  function query(root, scope) {
    const doc = scope || document;
    return {
      trigger: root.querySelector('[data-companion-trigger]'),
      bubble: root.querySelector('[data-companion-bubble]'),
      bubbleText: root.querySelector('[data-companion-bubble-text]'),
      panel: doc.querySelector('[data-companion-panel]'),
      close: doc.querySelector('[data-companion-close]'),
      newChat: doc.querySelector('[data-companion-new-chat]'),
      reset: doc.querySelector('[data-companion-reset-position]'),
      context: doc.querySelector('[data-companion-context]'),
      thread: doc.querySelector('[data-companion-thread]'),
      form: doc.querySelector('[data-companion-form]'),
      input: doc.querySelector('[data-companion-input]'),
      send: doc.querySelector('[data-companion-send]'),
      status: doc.querySelector('[data-companion-status]')
    };
  }

  function setStatus(els, message) {
    els.status.textContent = message;
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(text || ''));
    return div.innerHTML;
  }

  function renderMarkdown(text) {
    let html = escapeHtml(text);
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/(?:^|\n)[\s]*[-*]\s+(.+?)(?=\n|$)/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)(\n?)/g, function (match) {
      return '<ul>' + match.trim() + '</ul>';
    });
    html = html.replace(/<\/ul>\n?<ul>/g, '');
    html = html.replace(/\n/g, '<br>');
    return html;
  }

  function scrollThread(els) {
    els.thread.scrollTop = els.thread.scrollHeight;
  }

  function addMessage(els, role, content) {
    const message = document.createElement('div');
    message.className = 'companion-message companion-message-' + role;
    message.setAttribute('data-companion-message', '');

    const body = document.createElement('div');
    body.className = 'companion-message-content';
    body.setAttribute('data-companion-message-content', '');
    body.innerHTML = renderMarkdown(content);

    message.appendChild(body);
    els.thread.appendChild(message);
    scrollThread(els);
    return message;
  }

  function resizeInput(inputEl) {
    inputEl.style.height = 'auto';
    inputEl.style.height = Math.min(inputEl.scrollHeight, 118) + 'px';
  }

  return {
    query: query,
    setStatus: setStatus,
    renderMarkdown: renderMarkdown,
    scrollThread: scrollThread,
    addMessage: addMessage,
    resizeInput: resizeInput
  };
})();
