/* Weekly plan: assign form, move up/down, SortableJS drag-drop */
(function () {
  'use strict';

  function csrfHeaders() {
    var token = document.querySelector('meta[name="_csrf"]')?.content;
    var header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
    if (token) headers[header] = token;
    return headers;
  }

  function encode(values) {
    var body = new URLSearchParams();
    Object.keys(values).forEach(function (key) {
      var value = values[key];
      if (Array.isArray(value)) {
        value.forEach(function (item) { body.append(key, item); });
        return;
      }
      body.append(key, value);
    });
    return body.toString();
  }

  function request(method, url, values) {
    return window.fetch(url, {
      method: method,
      headers: csrfHeaders(),
      body: encode(values || {})
    }).then(function (response) {
      if (!response.ok) throw new Error('weekly-plan-request-failed');
      return response;
    });
  }

  function orderFor(dropzone) {
    return Array.from(dropzone.querySelectorAll('[data-plan-id]'))
      .map(function (item) { return item.dataset.planId; })
      .filter(Boolean);
  }

  function updateMoveButtons(dropzone) {
    var items = Array.from(dropzone.querySelectorAll('[data-plan-id]'));
    items.forEach(function (item, index) {
      var up = item.querySelector('[data-weekly-move="up"]');
      var down = item.querySelector('[data-weekly-move="down"]');
      if (up) up.disabled = index === 0;
      if (down) down.disabled = index === items.length - 1;
    });
  }

  function reorder(dropzone) {
    var dayOfWeek = dropzone.dataset.day;
    var order = orderFor(dropzone);
    if (!dayOfWeek || order.length === 0) return Promise.resolve();
    updateMoveButtons(dropzone);
    return request('PUT', '/weekly-plan/reorder', { dayOfWeek: dayOfWeek, order: order });
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.day-dropzone').forEach(updateMoveButtons);

    var form = document.getElementById('weekly-assign-form');
    if (form) {
      form.addEventListener('submit', function (event) {
        event.preventDefault();
        var status = document.getElementById('weekly-form-status');
        var data = new FormData(form);
        var garmentId = data.get('garmentId');
        var dayOfWeek = data.get('dayOfWeek');
        if (!garmentId || !dayOfWeek) return;
        var button = form.querySelector('button[type="submit"]');
        if (button) button.disabled = true;
        if (status) status.textContent = 'Asignando prenda al plan.';
        request('POST', '/weekly-plan/assign', { garmentId: garmentId, dayOfWeek: dayOfWeek, position: 999 })
          .then(function () { window.location.reload(); })
          .catch(function () {
            if (status) status.textContent = 'No se pudo asignar la prenda.';
            if (button) button.disabled = false;
          });
      });
    }

    document.addEventListener('click', function (event) {
      var move = event.target.closest('[data-weekly-move]');
      if (!move) return;
      var item = move.closest('[data-plan-id]');
      var dropzone = move.closest('.day-dropzone');
      if (!item || !dropzone) return;

      if (move.dataset.weeklyMove === 'up') {
        var previous = item.previousElementSibling;
        while (previous && !previous.matches('[data-plan-id]')) previous = previous.previousElementSibling;
        if (previous) dropzone.insertBefore(item, previous);
      }

      if (move.dataset.weeklyMove === 'down') {
        var next = item.nextElementSibling;
        while (next && !next.matches('[data-plan-id]')) next = next.nextElementSibling;
        if (next) dropzone.insertBefore(next, item);
      }

      reorder(dropzone).catch(function () { window.location.reload(); });
      item.focus({ preventScroll: true });
    });

    document.querySelectorAll('.day-dropzone').forEach(function (el) {
      new Sortable(el, {
        group: { name: 'weekly-plan', put: true },
        animation: 150,
        ghostClass: 'sortable-ghost',
        chosenClass: 'sortable-chosen',
        dragClass: 'sortable-drag',
        onEnd: function (evt) {
          var sameDayReorder = evt.from === evt.to;
          var dayOfWeek = evt.to.dataset.day;
          if (!dayOfWeek) return;

          if (sameDayReorder) {
            var order = Array.from(evt.to.querySelectorAll('[data-plan-id]'))
              .map(function (item) { return item.dataset.planId; })
              .filter(Boolean);

            request('PUT', '/weekly-plan/reorder', { dayOfWeek: dayOfWeek, order: order })
              .catch(function () { window.location.reload(); });
            return;
          }

          var garmentId = evt.item.dataset.garmentId;
          var newPosition = evt.newIndex;
          if (!garmentId) return;

          request('POST', '/weekly-plan/assign', { garmentId: garmentId, dayOfWeek: dayOfWeek, position: newPosition })
            .then(function () { window.location.reload(); })
            .catch(function () { window.location.reload(); });
        }
      });
    });

    var pool = document.getElementById('garment-pool');
    if (pool) {
      new Sortable(pool, {
        group: { name: 'weekly-plan', pull: 'clone', put: false },
        animation: 150,
        sort: false,
        ghostClass: 'sortable-ghost',
        chosenClass: 'sortable-chosen',
        dragClass: 'sortable-drag'
      });
    }
  });
})();
