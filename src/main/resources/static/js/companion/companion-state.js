window.CompanionState = (function () {
  'use strict';

  return {
    STORAGE_KEY: 'colorinchi.companion.v1',
    DEFAULT_TITLE: 'Ayudante Colorinchi',
    STREAM_TIMEOUT: 60000,
    createState: function () {
      return {
        sessionId: null,
        eventSource: null,
        accumulated: '',
        streamingEl: null,
        isSending: false,
        isOpen: false,
        dragMoved: false,
        previouslyFocused: null,
        retryCount: 0,
        streamTimeout: null,
        streamResolved: false,
        isResetting: false,
        anchorRect: null
      };
    }
  };
})();
