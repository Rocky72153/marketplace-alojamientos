import test from 'node:test';
import assert from 'node:assert/strict';
import { initBooking } from '../src/booking.js';

class MockElement {
  constructor(tagName) {
    this.tagName = tagName;
    this.children = [];
    this.listeners = {};
    this.attributes = {};
    this.value = '';
    this.checked = false;
    this.disabled = false;
    this.hidden = false;
    this.textContent = '';
  }

  append(...nodes) { this.children.push(...nodes); }
  prepend(...nodes) { this.children.unshift(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(name, handler) { this.listeners[name] = handler; }
  reportValidity() { return true; }
  async dispatch(name, event = {}) {
    event.preventDefault ||= () => {};
    return this.listeners[name]?.(event);
  }
}

function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) {
    const found = find(child, predicate);
    if (found) return found;
  }
  return null;
}

test('cotiza extras del alojamiento y confirma con una clave idempotente de pago simulado', async () => {
  const container = new MockElement('div');
  const accountPanel = new MockElement('section');
  const documentRef = {
    createElement: (tagName) => new MockElement(tagName),
    querySelector: (selector) => selector === '#account-panel' ? accountPanel : null,
  };
  const requests = [];
  let historyRefreshes = 0;
  const fetchImpl = async (url, options) => {
    requests.push({ url: url.toString(), options });
    if (url.pathname.endsWith('/quotes')) {
      return { ok: true, async json() {
        return { nights: 2, pricePerNight: 100000, nightlySubtotal: 200000, cleaningFee: 35000,
          serviceFee: 20000, discountAmount: 0, extras: [{ name: 'Desayuno', quantity: 2, lineTotal: 40000 }],
          total: 295000, freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 };
      } };
    }
    return { ok: true, async json() { return { id: 'reservation-123', paymentStatus: 'TEST_APPROVED' }; } };
  };
  const listing = { id: 'listing-1', title: 'Casa tranquila', capacity: 4,
    extraOptions: [{ id: 'extra-1', name: 'Desayuno', unitPrice: 20000 }] };
  const flow = initBooking(container, listing, 'http://localhost:8080', () => 'guest-token',
    async () => { historyRefreshes += 1; }, documentRef, fetchImpl);

  const arrival = find(flow.form, (node) => node.name === 'checkIn');
  const departure = find(flow.form, (node) => node.name === 'checkOut');
  const extra = find(flow.form, (node) => node.type === 'checkbox');
  const quantity = find(flow.form, (node) => node.type === 'number' && node !== find(flow.form, (item) => item.name === 'guests'));
  arrival.value = '2026-12-10';
  departure.value = '2026-12-12';
  extra.checked = true;
  extra.value = 'extra-1';
  quantity.value = '2';
  await flow.form.dispatch('submit');

  assert.equal(requests[0].url, 'http://localhost:8080/api/listings/listing-1/quotes');
  assert.deepEqual(JSON.parse(requests[0].options.body).extras, [{ extraId: 'extra-1', quantity: 2 }]);
  assert.equal(flow.reserveButton.hidden, false);
  await flow.reserveButton.dispatch('click');

  assert.equal(requests[1].url, 'http://localhost:8080/api/reservations');
  assert.equal(requests[1].options.headers.Authorization, 'Bearer guest-token');
  assert.ok(requests[1].options.headers['Idempotency-Key']);
  assert.deepEqual(JSON.parse(requests[1].options.body).extras, [{ extraId: 'extra-1', quantity: 2 }]);
  assert.equal(historyRefreshes, 1);
  assert.match(find(flow.section, (node) => node.className === 'booking-feedback').textContent, /no se realizó ningún cobro real/i);
});
