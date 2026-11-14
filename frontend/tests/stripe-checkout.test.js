import test from 'node:test';
import assert from 'node:assert/strict';
import { fetchPaymentMode, initStripeReturn, stripeCheckoutUrl } from '../src/stripe-checkout.js';
import { initBooking } from '../src/booking.js';

class Element {
  constructor(tag) { this.tagName = tag; this.children = []; this.listeners = {}; this.attributes = {}; this.value = ''; this.disabled = false; this.hidden = false; this.checked = false; this.textContent = ''; }
  append(...items) { this.children.push(...items); }
  prepend(...items) { this.children.unshift(...items); }
  replaceChildren(...items) { this.children = items; }
  setAttribute(key, value) { this.attributes[key] = value; }
  removeAttribute(key) { delete this[key]; }
  addEventListener(name, fn) { this.listeners[name] = fn; }
  reportValidity() { return true; }
  async dispatch(name) { return this.listeners[name]?.({ preventDefault() {} }); }
}
const doc = { createElement: (tag) => new Element(tag), querySelector: () => null };
const attemptId = '17dbe8dc-b789-4a7c-a64b-b3a784d114a0';
const quote = { nights: 2, pricePerNight: 100000, nightlySubtotal: 200000, cleaningFee: 0,
  serviceFee: 0, discountAmount: 0, total: 200000, extras: [], freeRefundDays: 7,
  partialRefundDays: 2, partialRefundPercentage: 50 };
const response = (body, ok = true) => ({ ok, json: async () => body });
function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) { const result = find(child, predicate); if (result) return result; }
  return null;
}

test('solo acepta direcciones HTTPS de Checkout de Stripe', () => {
  assert.equal(stripeCheckoutUrl('https://checkout.stripe.com/c/pay/cs_test_123'), 'https://checkout.stripe.com/c/pay/cs_test_123');
  for (const invalid of ['http://checkout.stripe.com/x', 'https://checkout.stripe.com.evil.invalid/x',
    'https://evil.invalid/checkout.stripe.com', 'https://user@checkout.stripe.com/x', 'javascript:alert(1)', '']) {
    assert.equal(stripeCheckoutUrl(invalid), null);
  }
});

test('sin configuración válida no se elige un modo de pago', async () => {
  assert.equal(await fetchPaymentMode('http://localhost:8080', async () => response({ mode: 'SIMULATED_TEST' })), 'SIMULATED_TEST');
  await assert.rejects(fetchPaymentMode('http://localhost:8080', async () => response({}, false)));
  await assert.rejects(fetchPaymentMode('http://localhost:8080', async () => response({ mode: 'LIVE' })));
  let calls = 0;
  const flow = initBooking(new Element('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080', () => 'guest',
    () => {}, doc, async () => { calls += 1; return response(quote); }, { getPaymentMode: () => 'UNAVAILABLE' });
  await flow.form.dispatch('submit');
  assert.equal(flow.reserveButton.disabled, true);
  await flow.reserveButton.dispatch('click');
  assert.equal(calls, 1);
});

test('Stripe reintenta la misma operación y solo navega al dominio verificado', async () => {
  const requests = [];
  const navigations = [];
  let confirmed = 0;
  const fetch = async (url, options) => {
    requests.push({ url, options });
    if (url.pathname.endsWith('/quotes')) return response(quote);
    if (requests.length === 2) return response({ attemptId, status: 'PREPARING', checkoutUrl: null });
    return response({ attemptId, status: 'OPEN', checkoutUrl: 'https://checkout.stripe.com/c/pay/cs_test_123' });
  };
  const flow = initBooking(new Element('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080', () => 'guest',
    () => { confirmed += 1; }, doc, fetch, { getPaymentMode: () => 'STRIPE_TEST', navigate: (url) => navigations.push(url) });
  await flow.form.dispatch('submit');
  await flow.reserveButton.dispatch('click');
  assert.equal(navigations.length, 0);
  await flow.reserveButton.dispatch('click');
  assert.equal(requests[1].url.pathname, '/api/stripe-test/checkouts');
  assert.equal(requests[1].options.headers.Authorization, 'Bearer guest');
  assert.equal(requests[1].options.headers['Idempotency-Key'], requests[2].options.headers['Idempotency-Key']);
  assert.equal(JSON.parse(requests[1].options.body).listingId, 'listing');
  assert.deepEqual(navigations, ['https://checkout.stripe.com/c/pay/cs_test_123']);
  assert.equal(confirmed, 0);
});

test('no navega a una URL ajena aunque el servidor la devuelva', async () => {
  const navigations = [];
  const fetch = async (url) => url.pathname.endsWith('/quotes') ? response(quote)
    : response({ attemptId, status: 'OPEN', checkoutUrl: 'https://checkout.stripe.com.evil.invalid/steal' });
  const flow = initBooking(new Element('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080', () => 'guest',
    () => {}, doc, fetch, { getPaymentMode: () => 'STRIPE_TEST', navigate: (url) => navigations.push(url) });
  await flow.form.dispatch('submit'); await flow.reserveButton.dispatch('click');
  assert.equal(navigations.length, 0);
  assert.match(find(flow.section, (node) => node.className === 'booking-feedback').textContent, /página de pago válida/i);
});

test('el retorno consulta el servidor y solo confirma con estado PAID', async () => {
  const container = new Element('section');
  let state = 'OPEN'; let confirmed = 0; const calls = [];
  const view = initStripeReturn(container, attemptId, 'http://localhost:8080', () => 'guest', () => { confirmed += 1; }, doc,
    async (url, options) => { calls.push({ url, options }); return response({ attemptId, status: state, checkoutUrl: 'https://checkout.stripe.com/c/pay/cs_test_123' }); });
  await view.refresh();
  assert.equal(calls[0].options.headers.Authorization, 'Bearer guest');
  assert.match(container.children[1].textContent, /pendiente/);
  assert.equal(confirmed, 0);
  state = 'PAID'; await view.refresh();
  assert.match(container.children[1].textContent, /confirmado por el servidor/i);
  assert.equal(confirmed, 1);
  await view.refresh();
  assert.equal(confirmed, 1);
});

test('el retorno descarta una respuesta tras cambiar o cerrar la sesión', async () => {
  const container = new Element('section');
  let token = 'guest-one'; let resolve; let confirmed = 0;
  const view = initStripeReturn(container, attemptId, 'http://localhost:8080', () => token, () => { confirmed += 1; }, doc,
    () => new Promise((r) => { resolve = r; }));
  const old = view.refresh(); token = null; await view.refresh();
  resolve(response({ attemptId, status: 'PAID' })); await old;
  assert.equal(confirmed, 0);
  assert.match(container.children[1].textContent, /inicia sesión/i);
});

test('un reembolso incierto muestra que requiere revisión, sin afirmar que fue procesado', async () => {
  const container = new Element('section');
  let confirmed = 0;
  const view = initStripeReturn(container, attemptId, 'http://localhost:8080', () => 'guest',
    () => { confirmed += 1; }, doc, async () => response({ attemptId, status: 'REFUND_REVIEW' }));
  await view.refresh();
  assert.match(container.children[1].textContent, /reembolso de prueba requiere revisión/i);
  assert.doesNotMatch(container.children[1].textContent, /fue reembolsado/i);
  assert.equal(confirmed, 0);
});
