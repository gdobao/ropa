/* CSRF header injection for HTMX + 429 rate-limit toast */
(function () {
  'use strict';

  document.addEventListener('htmx:configRequest', function (e) {
    var token = document.querySelector('meta[name="_csrf"]')?.content;
    var header = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (token && header) e.detail.headers[header] = token;
  });

  document.addEventListener('htmx:responseError', function (e) {
    if (e.detail.xhr.status === 429) {
      var toast = document.getElementById('rate-limit-toast');
      if (!toast) {
        var div = document.createElement('div');
        div.id = 'rate-limit-toast';
        div.className = 'notice error';
        div.style.cssText = 'position:fixed;top:var(--space-md);left:50%;transform:translateX(-50%);z-index:9999;max-width:400px;text-align:center;';
        div.innerHTML = '<p><strong>Demasiadas solicitudes</strong></p><p style="font-size:.875rem;">Espera unos minutos antes de intentarlo de nuevo.</p><a href="/wardrobe" class="btn" style="margin-top:var(--space-sm);">Volver al armario</a>';
        document.body.insertBefore(div, document.body.firstChild);
      }
      setTimeout(function () {
        var t = document.getElementById('rate-limit-toast');
        if (t) t.remove();
      }, 8000);
    }
  });
})();
