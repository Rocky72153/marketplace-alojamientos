import test from 'node:test';
import assert from 'node:assert/strict';
import { initAccount } from '../src/account.js';

test('abre inicio de sesión si alguien sin cuenta pide ver sus reservas', () => {
  const previousStorage = globalThis.localStorage;
  const handlers = {};
  globalThis.localStorage = { getItem: () => null, removeItem() {} };
  const panel = { hidden: true, scrollIntoView() {} };
  const feedback = { textContent: '' };
  const reservationLink = {
    addEventListener(name, handler) { handlers[name] = handler; },
  };
  const documentRef = {
    querySelector(selector) {
      return {
        '#account-panel': panel,
        '#account-feedback': feedback,
        '#reservation-link': reservationLink,
      }[selector] || null;
    },
  };

  try {
    initAccount(documentRef);
    let prevented = false;
    handlers.click({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(panel.hidden, false);
    assert.match(feedback.textContent, /inicia sesión/i);
  } finally {
    if (previousStorage === undefined) delete globalThis.localStorage;
    else globalThis.localStorage = previousStorage;
  }
});
