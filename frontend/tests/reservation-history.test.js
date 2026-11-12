import test from 'node:test';
import assert from 'node:assert/strict';
import { renderReservationHistory } from '../src/account.js';

class MockElement {
  constructor(tagName) {
    this.tagName = tagName;
    this.children = [];
    this.listeners = {};
    this.hidden = false;
    this.disabled = false;
    this.textContent = '';
  }

  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  addEventListener(name, handler) { this.listeners[name] = handler; }
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

function reservation(id, status = 'CONFIRMED') {
  return {
    id, listingTitle: 'Casa de prueba', checkIn: '2026-12-10', checkOut: '2026-12-12', nights: 2,
    total: 255000, status, paymentStatus: status === 'CONFIRMED' ? 'TEST_APPROVED' : 'TEST_REFUNDED',
    refundPercentage: status === 'CONFIRMED' ? null : 100, refundAmount: status === 'CONFIRMED' ? null : 255000,
  };
}

function historyFixture(fetchImpl) {
  const container = new MockElement('section');
  const feedback = new MockElement('p');
  const items = new MockElement('div');
  const documentRef = {
    createElement: (tagName) => new MockElement(tagName),
    querySelector(selector) {
      return {
        '#reservation-history-feedback': feedback,
        '#reservation-history-items': items,
      }[selector] || null;
    },
  };
  return { container, feedback, items, documentRef, fetchImpl };
}

test('muestra el historial, el reembolso y refresca la lista después de cancelar', async () => {
  const requests = [];
  let historyCalls = 0;
  const fixture = historyFixture(async (url, options = {}) => {
    requests.push({ url: url.toString(), options });
    if (url.pathname === '/api/reservations') {
      historyCalls += 1;
      return { ok: true, async json() { return historyCalls === 1 ? [reservation('active'), reservation('cancelled', 'CANCELLED')] : []; } };
    }
    return { ok: true, async json() { return { refundPercentage: 100, refundAmount: 255000 }; } };
  });

  await renderReservationHistory(fixture.container, 'http://localhost:8080', 'guest-token', fixture.fetchImpl, fixture.documentRef);

  assert.equal(fixture.container.hidden, false);
  assert.equal(fixture.items.children.length, 2);
  assert.match(find(fixture.items, (node) => /Reembolso simulado/.test(node.textContent))?.textContent, /100%/);
  const form = find(fixture.items, (node) => node.tagName === 'form');
  const reason = find(form, (node) => node.tagName === 'textarea');
  reason.value = 'Cambio de planes';
  await form.dispatch('submit');

  assert.equal(requests[1].url, 'http://localhost:8080/api/reservations/active/cancel');
  assert.equal(requests[1].options.headers.Authorization, 'Bearer guest-token');
  assert.deepEqual(JSON.parse(requests[1].options.body), { reason: 'Cambio de planes' });
  assert.equal(fixture.items.children.length, 1);
  assert.match(fixture.items.children[0].textContent, /Aún no tienes reservas/i);
  assert.match(fixture.feedback.textContent, /Cancelación registrada/i);
});

test('mantiene el formulario disponible y explica el error si la cancelación falla', async () => {
  const fixture = historyFixture(async (url) => {
    if (url.pathname === '/api/reservations') return { ok: true, async json() { return [reservation('active')]; } };
    return { ok: false, async json() { return { message: 'La reserva ya no se puede cancelar.' }; } };
  });

  await renderReservationHistory(fixture.container, 'http://localhost:8080', 'guest-token', fixture.fetchImpl, fixture.documentRef);
  const form = find(fixture.items, (node) => node.tagName === 'form');
  const button = find(form, (node) => node.tagName === 'button');
  await form.dispatch('submit');

  assert.equal(button.disabled, false);
  assert.match(fixture.feedback.textContent, /ya no se puede cancelar/i);
});

test('descarta el historial que llega después de cerrar sesión', async () => {
  let resolveRequest;
  const fixture = historyFixture(() => new Promise((resolve) => { resolveRequest = resolve; }));
  const previous = renderReservationHistory(fixture.container, 'http://localhost:8080', 'token-anterior',
    fixture.fetchImpl, fixture.documentRef);
  await renderReservationHistory(fixture.container, 'http://localhost:8080', null, fixture.fetchImpl, fixture.documentRef);
  resolveRequest({ ok: true, async json() { return [reservation('privada')]; } });
  await previous;
  assert.equal(fixture.container.hidden, true);
  assert.equal(fixture.items.children.length, 0);
  assert.equal(fixture.feedback.textContent, '');
});

test('permite publicar una reseña desde una reserva completada', async () => {
  const completed = { ...reservation('reserva', 'COMPLETED'), listingId: 'alojamiento' };
  const requests = [];
  let created = false;
  const fixture = historyFixture(async (url, options = {}) => {
    requests.push({ url: url.toString(), options });
    if (url.pathname === '/api/reservations') return { ok: true, async json() { return [completed]; } };
    return { ok: true, async json() { return { id: 'reseña' }; } };
  });
  await renderReservationHistory(fixture.container, 'http://localhost:8080', 'guest-token', fixture.fetchImpl,
    fixture.documentRef, '', { getToken: () => 'guest-token', onReviewCreated: () => { created = true; } });

  const panel = find(fixture.items, (node) => node.className === 'review-form-panel');
  assert.ok(panel);
  const form = find(panel, (node) => node.tagName === 'form');
  find(form, (node) => node.tagName === 'textarea').value = 'Una buena estancia.';
  await form.dispatch('submit');
  assert.equal(requests[1].url, 'http://localhost:8080/api/listings/alojamiento/reviews');
  assert.deepEqual(JSON.parse(requests[1].options.body), { bookingId: 'reserva', rating: 5, content: 'Una buena estancia.' });
  assert.equal(requests[1].options.headers.Authorization, 'Bearer guest-token');
  assert.equal(created, true);
});

test('no vuelve a ofrecer una reseña al cargar una reserva ya reseñada', async () => {
  const completed = { ...reservation('reserva', 'COMPLETED'), listingId: 'alojamiento', reviewed: true };
  const fixture = historyFixture(async () => ({ ok: true, async json() { return [completed]; } }));
  await renderReservationHistory(fixture.container, 'http://localhost:8080', 'guest-token', fixture.fetchImpl,
    fixture.documentRef);
  assert.equal(find(fixture.items, (node) => node.className === 'review-form-panel'), null);
  assert.match(find(fixture.items, (node) => /Ya escribiste una reseña/.test(node.textContent))?.textContent,
    /esta reserva/);
});
