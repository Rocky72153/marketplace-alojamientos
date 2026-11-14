import test from 'node:test';
import assert from 'node:assert/strict';

test('el regreso a inicio consulta el intento autenticado y no confirma por parámetros de URL', async () => {
  const previous = { document: globalThis.document, window: globalThis.window, fetch: globalThis.fetch,
    localStorage: globalThis.localStorage };
  const attempt = '17dbe8dc-b789-4a7c-a64b-b3a784d114a0';
  const element = () => ({ children: [], hidden: false, textContent: '', setAttribute() {}, removeAttribute() {},
    replaceChildren(...children) { this.children = children; }, addEventListener() {} });
  const section = element();
  const calls = [];
  globalThis.document = { querySelector: (selector) => selector === '#payment-return' ? section : null, createElement: element };
  globalThis.window = { location: { search: `?stripe_checkout=${attempt}&result=success` } };
  globalThis.localStorage = { getItem: () => JSON.stringify({ accessToken: 'guest-token', expiresAt: '2099-01-01T00:00:00Z',
    user: { id: 'guest', name: 'Huésped', role: 'GUEST' } }), removeItem() {} };
  globalThis.fetch = async (url, options) => {
    calls.push({ path: url.pathname, options });
    return { ok: true, json: async () => ({ attemptId: attempt, status: 'OPEN' }) };
  };
  try {
    await import('../src/main.js?stripe-return-ui-test');
    await new Promise(setImmediate);
    assert.equal(section.hidden, false);
    assert.match(section.children[1].textContent, /pendiente/i);
    assert.equal(calls[0].path, `/api/stripe-test/checkouts/${attempt}`);
    assert.equal(calls[0].options.headers.Authorization, 'Bearer guest-token');
  } finally {
    globalThis.document = previous.document;
    globalThis.window = previous.window;
    globalThis.fetch = previous.fetch;
    globalThis.localStorage = previous.localStorage;
  }
});
