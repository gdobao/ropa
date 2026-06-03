/* Garment new: upload preview, validation, size warning */
(function () {
  'use strict';

  var form = document.getElementById('garment-upload-form');
  var input = document.getElementById('image-upload');
  var submit = document.getElementById('upload-submit');
  var preview = document.getElementById('upload-preview');
  var previewImage = document.getElementById('upload-preview-image');
  var meta = document.getElementById('upload-meta');
  var warning = document.getElementById('upload-warning');
  var error = document.getElementById('upload-error');
  if (!form || !input || !submit) return;

  var objectUrl = null;
  var maxSize = 8 * 1024 * 1024;
  var validTypes = ['image/jpeg', 'image/png', 'image/webp'];

  function formatSize(bytes) {
    return (bytes / (1024 * 1024)).toFixed(1).replace('.', ',') + ' MB';
  }

  function setError(message) {
    error.textContent = message || '';
    error.hidden = !message;
  }

  function setWarning(message) {
    warning.textContent = message || '';
    warning.hidden = !message;
  }

  function resetPreview() {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = null;
    preview.hidden = true;
    previewImage.removeAttribute('src');
    submit.disabled = true;
  }

  input.addEventListener('change', function () {
    var file = input.files && input.files[0];
    resetPreview();
    setError('');
    setWarning('');

    if (!file) {
      meta.textContent = 'Todavía no seleccionaste una imagen.';
      setError('Selecciona una imagen para continuar.');
      return;
    }

    meta.textContent = file.name + ' · ' + formatSize(file.size);

    if (!validTypes.includes(file.type)) {
      setError('El archivo debe ser JPG, PNG o WebP.');
      return;
    }

    if (file.size > maxSize) {
      setError('La imagen no puede superar los 8 MB.');
      return;
    }

    objectUrl = URL.createObjectURL(file);
    previewImage.onload = function () {
      preview.hidden = false;
      submit.disabled = false;
      if (previewImage.naturalWidth < 600 || previewImage.naturalHeight < 600) {
        setWarning('La imagen es pequeña. Si puedes, usa una foto de al menos 600 × 600 px para mejorar el análisis.');
      }
    };
    previewImage.src = objectUrl;
  });

  form.addEventListener('submit', function (event) {
    if (submit.disabled) {
      event.preventDefault();
      setError('Selecciona una imagen válida para continuar.');
      return;
    }
    submit.disabled = true;
    submit.textContent = 'Analizando...';
  });
})();
