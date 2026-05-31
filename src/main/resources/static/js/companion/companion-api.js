window.CompanionApi = (function () {
  'use strict';

  function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers[header] = token;
    return headers;
  }

  function readContext() {
    return fetch('/api/companion/context').then(function (response) {
      return response.ok ? response.json() : null;
    });
  }

  function listSessions() {
    return fetch('/api/companion/sessions').then(function (response) {
      return response.ok ? response.json() : [];
    });
  }

  function createSession(title) {
    return fetch('/api/companion/sessions', {
      method: 'POST',
      headers: csrfHeaders(),
      body: JSON.stringify({ title: title, model: '' })
    }).then(function (response) {
      if (!response.ok) throw new Error('Failed to create session');
      return response.json();
    });
  }

  function deleteSession(sessionId) {
    return fetch('/api/companion/sessions/' + sessionId, {
      method: 'DELETE',
      headers: csrfHeaders()
    });
  }

  function listMessages(sessionId) {
    return fetch('/api/companion/sessions/' + sessionId + '/messages').then(function (response) {
      return response.ok ? response.json() : [];
    });
  }

  function sendMessage(sessionId, content) {
    return fetch('/api/companion/sessions/' + sessionId + '/messages', {
      method: 'POST',
      headers: csrfHeaders(),
      body: JSON.stringify({ content: content, model: '' })
    }).then(function (response) {
      if (!response.ok) {
        return response.json().then(function (data) {
          throw new Error(data.message || 'No pude enviar el mensaje.');
        });
      }
      return response.json();
    });
  }

  return {
    csrfHeaders: csrfHeaders,
    readContext: readContext,
    listSessions: listSessions,
    createSession: createSession,
    deleteSession: deleteSession,
    listMessages: listMessages,
    sendMessage: sendMessage
  };
})();
