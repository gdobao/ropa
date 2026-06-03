/* Wardrobe: category chip sync, focus management, screen reader announcements, loading submit */
(function () {
  'use strict';

  document.body.addEventListener('htmx:afterSwap', function (evt) {
    if (evt.detail.target.id === 'wardrobe-grid') {
      var url = new URL(evt.detail.requestConfig.path, window.location.origin);
      var category = url.searchParams.get('category') || '';
      document.querySelectorAll('.chips .chip').forEach(function (chip) {
        var href = chip.getAttribute('hx-get') || '';
        var active = false;
        if (!category && !href.includes('category=')) {
          chip.classList.add('active');
          active = true;
        } else if (href.includes('category=' + category)) {
          chip.classList.add('active');
          active = true;
        } else {
          chip.classList.remove('active');
        }
        chip.setAttribute('aria-pressed', active ? 'true' : 'false');
      });
    }

    if (evt.detail.target.id === 'wardrobe-grid') {
      var firstCard = document.querySelector('#wardrobe-grid .card');
      if (firstCard) {
        var favBtn = firstCard.querySelector('.fav-btn');
        if (favBtn) favBtn.focus();
      }
    }

    var region = document.getElementById('live-region');
    if (!region) return;
    var countEl = document.querySelector('#wardrobe-grid .meta strong');
    if (countEl) {
      region.textContent = 'Mostrando ' + countEl.textContent + ' prendas';
    } else {
      var heading = document.querySelector('#wardrobe-grid h3, #wardrobe-grid h2');
      if (heading) {
        region.textContent = heading.textContent;
      }
    }
  });

  document.querySelectorAll('[data-loading-submit]').forEach(function (form) {
    form.addEventListener('submit', function () {
      var button = form.querySelector('[data-loading-button]');
      if (!button) return;
      window.setTimeout(function () {
        button.textContent = button.dataset.loadingText || 'Enviando…';
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
      }, 0);
    });
  });
})();
