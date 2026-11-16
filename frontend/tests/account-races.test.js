import test from 'node:test';
import assert from 'node:assert/strict';
import { initAccount } from '../src/account.js';

test('una respuesta anterior de inicio de sesión no sustituye la cuenta más reciente ni revive una sesión cerrada', async () => {
  const oldStorage = globalThis.localStorage;
  const oldFormData = globalThis.FormData;
  const handlers = {};
  const values = new Map();
  globalThis.localStorage = { getItem: (key) => values.get(key) || null, setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) };
  globalThis.FormData = class { entries() { return [['email', 'demo@example.invalid']]; } };
  const form = { addEventListener(name, callback) { handlers[name] = callback; }, reset() {} };
  const requests = [];
  const doc = { querySelector: (selector) => selector === '#login-form' ? form : null };
  const account = initAccount(doc, 'http://localhost:8080', () => {}, () => new Promise((resolve) => requests.push(resolve)));
  const response = (name) => ({ ok: true, json: async () => ({ accessToken: name, user: { id: name, name, role: 'GUEST' }, expiresAt: '2099-01-01T00:00:00Z' }) });
  try {
    handlers.submit({ preventDefault() {} });
    handlers.submit({ preventDefault() {} });
    requests[1](response('segunda'));
    await new Promise(setImmediate);
    requests[0](response('primera'));
    await new Promise(setImmediate);
    assert.equal(account.getSession().user.name, 'segunda');
    assert.equal(JSON.parse(values.get('marketplace-session')).user.name, 'segunda');
    handlers.submit({ preventDefault() {} });
    account.logout();
    requests[2](response('tardía'));
    await new Promise(setImmediate);
    assert.equal(account.getSession(), null);
    assert.equal(values.has('marketplace-session'), false);
  } finally {
    globalThis.localStorage = oldStorage;
    globalThis.FormData = oldFormData;
  }
});

test('rechaza sesiones almacenadas sin una fecha de vencimiento válida', () => {
  const oldStorage = globalThis.localStorage;
  let removed = false;
  globalThis.localStorage = { getItem: () => JSON.stringify({ accessToken: 'x', user: { name: 'x' }, expiresAt: 'inválida' }), removeItem: () => { removed = true; } };
  try {
    const account = initAccount({ querySelector: () => null });
    assert.equal(account.getSession(), null);
    assert.equal(removed, true);
  } finally { globalThis.localStorage = oldStorage; }
});

test('evita registrar dos veces una cuenta ante un doble envío', async () => {
  const oldStorage = globalThis.localStorage;
  const oldFormData = globalThis.FormData;
  const values = new Map();
  globalThis.localStorage = { getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) };
  globalThis.FormData = class { entries() { return [['email', 'demo@example.invalid']]; } };
  let submit;
  const button = { disabled: false };
  const form = { addEventListener(name, callback) { if (name === 'submit') submit = callback; },
    querySelector: () => button, reset() {} };
  const requests = [];
  const account = initAccount({ querySelector: (selector) => selector === '#register-form' ? form : null },
    'http://localhost:8080', () => {}, () => new Promise((resolve) => requests.push(resolve)));
  try {
    submit({ preventDefault() {} });
    assert.equal(button.disabled, true);
    submit({ preventDefault() {} });
    assert.equal(requests.length, 1);
    requests[0]({ ok: true, json: async () => ({ accessToken: 'new-token',
      user: { id: 'new-user', name: 'Nueva cuenta', role: 'GUEST' }, expiresAt: '2099-01-01T00:00:00Z' }) });
    await new Promise(setImmediate);
    assert.equal(account.getSession().user.name, 'Nueva cuenta');
    assert.equal(button.disabled, false);
  } finally {
    globalThis.localStorage = oldStorage;
    globalThis.FormData = oldFormData;
  }
});
