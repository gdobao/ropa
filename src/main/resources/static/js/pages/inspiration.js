/* Inspiration: tag filter and text search */
(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    var search = document.querySelector('[data-inspiration-search]');
    var cards = Array.from(document.querySelectorAll('[data-inspiration-card]'));
    var empty = document.getElementById('inspiration-empty-results');
    var activeTag = '';

    function normalize(value) {
      return (value || '').toLocaleLowerCase('es-ES').normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function setActiveTag(tag) {
      activeTag = tag || '';
      document.querySelectorAll('[data-tag-filter]').forEach(function (button) {
        var isActive = (button.dataset.tagFilter || '') === activeTag;
        button.classList.toggle('active', isActive);
        button.setAttribute('aria-pressed', isActive ? 'true' : 'false');
      });
      applyFilters();
    }

    function cardTags(card) {
      return Array.from(card.querySelectorAll('.inspiration-chips [data-tag-filter]'))
        .map(function (button) { return button.dataset.tagFilter || button.textContent.trim(); });
    }

    function applyFilters() {
      var query = normalize(search ? search.value : '');
      var visibleCount = 0;
      cards.forEach(function (card) {
        var haystack = normalize(card.innerText);
        var tagMatch = !activeTag || cardTags(card).some(function (tag) { return tag === activeTag; });
        var searchMatch = !query || haystack.includes(query);
        var visible = tagMatch && searchMatch;
        card.hidden = !visible;
        if (visible) visibleCount += 1;
      });
      if (empty) empty.hidden = visibleCount !== 0;
    }

    document.addEventListener('click', function (event) {
      var button = event.target.closest('[data-tag-filter]');
      if (!button) return;
      setActiveTag(button.dataset.tagFilter || '');
    });

    if (search) {
      search.addEventListener('input', applyFilters);
    }
  });
})();
