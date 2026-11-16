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
  removeAttribute(name) { delete this.attributes[name]; }
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

test('cuentas de anfitrión y administrador pueden cotizar sin ofrecer una reserva que el servidor rechazaría', async () => {
  for (const role of ['HOST', 'ADMIN']) {
    let requests = 0;
    const container = new MockElement('div');
    const flow = initBooking(container, { id: 'listing', capacity: 4 }, 'http://localhost:8080', () => 'token', () => {},
      { createElement: (tag) => new MockElement(tag) }, async () => {
        requests += 1;
        return { ok: true, json: async () => ({ nights: 2, pricePerNight: 100000, nightlySubtotal: 200000,
          cleaningFee: 0, serviceFee: 0, discountAmount: 0, total: 200000, extras: [],
          freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 }) };
      }, { getRole: () => role, getPaymentMode: () => 'STRIPE_TEST' });
    await flow.form.dispatch('submit');
    assert.equal(flow.form.hidden, true);
    assert.equal(flow.reserveButton.disabled, true);
    await flow.reserveButton.dispatch('click');
    assert.equal(requests, 1);
    assert.match(find(container, (item) => item.className === 'booking-feedback').textContent, /cuenta de huésped/);
    await find(container, (item) => item.className?.includes('booking-edit')).dispatch('click');
    assert.equal(flow.form.hidden, false);
    assert.equal(flow.reserveButton.hidden, true);
  }
});

test('la guía avanza al cotizar, permite reintentar un error y solo confirma tras éxito', async () => {
  const doc = { createElement: (tag) => new MockElement(tag) };
  let fail = true;
  const fetchImpl = async (url) => url.pathname.endsWith('/quotes')
    ? { ok: true, json: async () => ({ nights: 2, pricePerNight: 100000, nightlySubtotal: 200000,
      cleaningFee: 0, serviceFee: 0, discountAmount: 0, total: 200000, extras: [],
      freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 }) }
    : fail ? { ok: false, json: async () => ({ message: 'Error temporal.' }) }
      : { ok: true, json: async () => ({ id: 'reserva' }) };
  const flow = initBooking(new MockElement('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080',
    () => 'guest-token', () => {}, doc, fetchImpl);
  const progress = find(flow.section, (node) => node.className === 'booking-progress');
  const steps = progress.children[0].children;
  assert.equal(steps[0].attributes['aria-current'], 'step');
  assert.equal(steps[2].textContent, '3. Pago de prueba');
  await flow.form.dispatch('submit');
  assert.equal(steps[1].attributes['aria-current'], 'step');
  assert.match(flow.reserveButton.textContent, /200\.000/);
  await flow.reserveButton.dispatch('click');
  assert.equal(steps[2].attributes['aria-current'], 'step');
  assert.equal(steps[3].attributes['data-state'], 'upcoming');
  assert.equal(flow.reserveButton.disabled, false);
  assert.match(find(flow.section, (node) => node.className === 'booking-feedback').textContent, /intentarlo de nuevo/i);
  fail = false;
  await flow.reserveButton.dispatch('click');
  assert.equal(steps[3].attributes['aria-current'], 'step');
  assert.equal(steps[2].attributes['aria-current'], undefined);
});

test('recupera fechas y huéspedes de búsqueda y conserva cambios al solicitar acceso', async () => {
  const win = { location: new URL('http://localhost/?listing=listing-1&checkin=2099-12-10&checkout=2099-12-12&guests=3') };
  win.history = { replaceState(_state, _unused, url) { win.location = new URL(url); } };
  let prompted = false;
  const doc = { createElement: (tag) => new MockElement(tag), defaultView: win };
  const fetchImpl = async () => ({ ok: true, json: async () => ({ nights: 2, extras: [], total: 100000 }) });
  const flow = initBooking(new MockElement('div'), { id: 'listing-1', capacity: 4 }, 'http://localhost:8080',
    () => null, () => {}, doc, fetchImpl, { onRequireAccount: () => { prompted = true; } });
  const checkIn = find(flow.form, (node) => node.name === 'checkIn');
  const checkOut = find(flow.form, (node) => node.name === 'checkOut');
  const guests = find(flow.form, (node) => node.name === 'guests');
  assert.equal(checkIn.value, '2099-12-10');
  assert.equal(checkOut.value, '2099-12-12');
  assert.equal(guests.value, '3');
  checkIn.value = '2099-12-11';
  guests.value = '2';
  await flow.form.dispatch('submit');
  await flow.reserveButton.dispatch('click');
  assert.equal(prompted, true);
  assert.equal(win.location.searchParams.get('checkin'), '2099-12-11');
  assert.equal(win.location.searchParams.get('checkout'), '2099-12-12');
  assert.equal(win.location.searchParams.get('guests'), '2');
});

test('ignora fechas imposibles y cantidades fuera de capacidad en la URL', () => {
  const doc = { createElement: (tag) => new MockElement(tag), defaultView: {
    location: new URL('http://localhost/?checkin=2099-02-31&checkout=mal&guests=99'),
  } };
  const flow = initBooking(new MockElement('div'), { id: 'listing-1', capacity: 4 }, 'http://localhost:8080',
    () => null, () => {}, doc);
  assert.notEqual(find(flow.form, (node) => node.name === 'checkIn').value, '2099-02-31');
  assert.notEqual(find(flow.form, (node) => node.name === 'checkOut').value, 'mal');
  assert.equal(find(flow.form, (node) => node.name === 'guests').value, '2');
});

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
          total: 295000.5, freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 };
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
  assert.match(find(flow.section, (node) => node.tagName === 'strong' && /295\.000,50/.test(node.textContent)).textContent,
    /295\.000,50/);
});

test('descarta una cotización cuando cambian las fechas antes de recibirla', async () => {
  let resolveQuote;
  const documentRef = { createElement: (tag) => new MockElement(tag), querySelector: () => null };
  const fetchImpl = () => new Promise((resolve) => { resolveQuote = resolve; });
  const flow = initBooking(new MockElement('div'), { id: 'listing', capacity: 4 },
    'http://localhost:8080', () => 'guest-token', () => {}, documentRef, fetchImpl);
  const arrival = find(flow.form, (node) => node.name === 'checkIn');
  const request = flow.form.dispatch('submit');
  arrival.value = '2026-12-10';
  await arrival.dispatch('input');
  resolveQuote({ ok: true, async json() { return { nights: 2, extras: [], total: 100000 }; } });
  await request;
  assert.equal(flow.reserveButton.hidden, true);
  assert.equal(flow.quoteButton.disabled, false);
});

test('no muestra una reserva tardía a otra cuenta ni permite enviar dos confirmaciones', async () => {
  let token = 'guest-one';
  let resolveReservation;
  let reservationRequests = 0;
  let refreshes = 0;
  const documentRef = { createElement: (tag) => new MockElement(tag), querySelector: () => null };
  const fetchImpl = (url) => {
    if (url.pathname.endsWith('/quotes')) return Promise.resolve({ ok: true, async json() {
      return { nights: 2, pricePerNight: 100000, nightlySubtotal: 200000, cleaningFee: 0, serviceFee: 0,
        discountAmount: 0, total: 200000, extras: [], freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 };
    } });
    reservationRequests += 1;
    return new Promise((resolve) => { resolveReservation = resolve; });
  };
  const flow = initBooking(new MockElement('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080',
    () => token, () => { refreshes += 1; }, documentRef, fetchImpl);
  await flow.form.dispatch('submit');
  const pending = flow.reserveButton.dispatch('click');
  await flow.reserveButton.dispatch('click');
  assert.equal(reservationRequests, 1);
  assert.equal(find(flow.form, (node) => node.name === 'checkIn').disabled, true);
  token = 'guest-two';
  flow.syncAccount();
  resolveReservation({ ok: true, async json() { return { id: 'private-reservation' }; } });
  await pending;
  assert.equal(refreshes, 0);
  assert.doesNotMatch(find(flow.section, (node) => node.className === 'booking-feedback').textContent, /private-reservation/);
  assert.equal(flow.reserveButton.hidden, true);
  assert.equal(find(flow.form, (node) => node.name === 'checkIn').disabled, false);
});

test('mantiene confirmada la reserva si falla la actualización del historial', async () => {
  const documentRef = { createElement: (tag) => new MockElement(tag), querySelector: () => null };
  const fetchImpl = async (url) => url.pathname.endsWith('/quotes')
    ? { ok: true, json: async () => ({ nights: 2, pricePerNight: 100000, nightlySubtotal: 200000,
      cleaningFee: 0, serviceFee: 0, discountAmount: 0, total: 200000, extras: [],
      freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 }) }
    : { ok: true, json: async () => ({ id: 'reserva-confirmada' }) };
  const flow = initBooking(new MockElement('div'), { id: 'listing', capacity: 4 }, 'http://localhost:8080',
    () => 'guest-token', () => { throw new Error('historial no disponible'); }, documentRef, fetchImpl);
  await flow.form.dispatch('submit');
  await flow.reserveButton.dispatch('click');
  const feedback = find(flow.section, (node) => node.className === 'booking-feedback').textContent;
  assert.match(feedback, /reserva confirmada.*reserva-confirmada/i);
  assert.match(feedback, /no fue posible actualizar el historial/i);
  assert.equal(flow.reserveButton.hidden, true);
});
