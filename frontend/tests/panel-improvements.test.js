import test from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { initAccount, renderReservationHistory } from '../src/account.js';
import { initHostWorkspace } from '../src/host.js';
import { initAdministration } from '../src/ep09-admin.js';

const ok = (body) => ({ ok: true, json: async () => body });
const settle = () => new Promise(setImmediate);
function page(t, html = '<main></main>') {
  const dom = new JSDOM(html, { url: 'http://marketplace.test/' });
  t.after(() => dom.window.close());
  return { doc: dom.window.document, win: dom.window };
}

test('historial filtra alojamiento, código y estado sin perder cancelación ni reseñas', async (t) => {
  const { doc, win } = page(t, '<section id="history"><p id="reservation-history-feedback"></p><div id="reservation-history-items"></div></section>');
  const container = doc.querySelector('#history');
  const reservations = [
    { id: 'confirmada', listingTitle: 'Casa del centro', status: 'CONFIRMED', paymentStatus: 'TEST_APPROVED', total: 100000 },
    { id: 'terminada', listingTitle: 'Cabaña del río', listingId: 'l2', status: 'COMPLETED', paymentStatus: 'TEST_APPROVED', total: 200000 },
  ];
  await renderReservationHistory(container, win.location.origin, 'guest', async () => ok(reservations), doc);
  assert.equal(doc.querySelectorAll('.reservation-card').length, 2);
  assert.equal(doc.querySelector('details').open, false);
  assert.equal(doc.querySelectorAll('.status-badge').length, 2);
  const search = doc.querySelector('[name="reservationQuery"]'); search.value = 'cabana'; search.dispatchEvent(new win.Event('input'));
  assert.equal(doc.querySelectorAll('.reservation-card').length, 1);
  assert.ok(doc.querySelector('.review-form-panel'));
  const state = doc.querySelector('[name="reservationStatus"]'); state.value = 'CONFIRMED'; state.dispatchEvent(new win.Event('change'));
  assert.match(doc.querySelector('.empty-state').textContent, /coincidan/);
  search.value = 'confirmada'; search.dispatchEvent(new win.Event('input'));
  assert.ok(doc.querySelector('.cancel-form'));
  assert.match(doc.querySelector('.workspace-count').textContent, /1 de 2 reservas/);
  await renderReservationHistory(container, win.location.origin, null, undefined, doc);
  assert.equal(doc.querySelector('.reservation-filters').hidden, true);
  assert.equal(doc.querySelectorAll('.reservation-card').length, 0);
});

test('anfitrión filtra la página y conserva recuento global real y navegación del servidor', async (t) => {
  const { doc, win } = page(t);
  const panel = initHostWorkspace(doc.querySelector('main'), win.location.origin,
    () => ({ accessToken: 'host', user: { role: 'HOST' } }), doc,
    async () => ok({ content: [
      { id: 'l1', title: 'Casa del centro', city: 'Bogotá', status: 'DRAFT', capacity: 2, pricePerNight: 100000 },
      { id: 'l2', title: 'Cabaña del río', city: 'Medellín', status: 'ACTIVE', capacity: 4, pricePerNight: 180000 },
    ], totalElements: 24, totalPages: 2 }));
  await panel.refresh();
  assert.equal(doc.querySelectorAll('.host-card').length, 2);
  assert.match(doc.querySelector('.workspace-count').textContent, /2 de 2.*24 alojamientos/);
  const search = doc.querySelector('[name="hostQuery"]'); search.value = 'medellin'; search.dispatchEvent(new win.Event('input'));
  assert.equal(doc.querySelectorAll('.host-card').length, 1);
  assert.match(doc.querySelector('.host-card').textContent, /Cabaña del río.*Activo/s);
  assert.ok([...doc.querySelectorAll('.host-card button')].some((node) => node.textContent === 'Pausar'));
  const state = doc.querySelector('[name="hostStatus"]'); state.value = 'DRAFT'; state.dispatchEvent(new win.Event('change'));
  assert.equal(doc.querySelectorAll('.host-card').length, 0);
  assert.match(doc.querySelector('.empty-state').textContent, /esta página/);
  assert.ok([...doc.querySelectorAll('button')].some((node) => node.textContent === 'Siguiente'));
  panel.clear(); assert.equal(search.value, '');
});

test('administración separa secciones y filtra tablas con acciones cerradas y paginación real', async (t) => {
  const { doc, win } = page(t);
  const panel = initAdministration(doc.querySelector('main'), win.location.origin, () => 'admin', doc, async (url) => {
    if (url.pathname === '/api/admin/dashboard') return ok({ users: 45, activeListings: 1 });
    if (url.pathname === '/api/admin/users') return ok({ content: [
      { id: 'u1', name: 'Ana', email: 'ana@test.invalid', role: 'HOST', accountState: 'ACTIVE' },
      { id: 'u2', name: 'José', email: 'jose@test.invalid', role: 'GUEST', accountState: 'RESTRICTED' },
    ], totalElements: 45, totalPages: 3 });
    if (url.pathname.endsWith('/manual-review')) return ok([]);
    return ok({ content: [], totalElements: 0, totalPages: 0 });
  });
  await panel.refresh();
  const overview = doc.querySelector('#admin-overview'); const users = doc.querySelector('#admin-users');
  assert.equal(overview.hidden, false); assert.equal(users.hidden, true);
  doc.querySelector('[aria-controls="admin-users"]').click();
  assert.equal(overview.hidden, true); assert.equal(users.hidden, false);
  assert.equal(users.querySelectorAll('table thead th').length, 4);
  assert.equal(users.querySelectorAll('tbody tr').length, 2);
  assert.equal(users.querySelector('details').open, false);
  const query = users.querySelector('input[type="search"]'); query.value = 'jose'; query.dispatchEvent(new win.Event('input'));
  assert.equal(users.querySelectorAll('tbody tr').length, 1);
  assert.match(users.querySelector('tbody').textContent, /José/);
  assert.match(users.querySelector('.admin-feedback').textContent, /1 de 2.*45 registros.*Página 1 de 3/);
  assert.ok([...users.querySelectorAll('button')].some((node) => node.textContent === 'Siguiente'));
  const state = users.querySelector('select'); state.value = 'ACTIVE'; state.dispatchEvent(new win.Event('change'));
  assert.match(users.querySelector('tbody').textContent, /No hay coincidencias/);
  panel.clear(); assert.equal(doc.querySelectorAll('tbody tr').length, 0);
});

test('inicio de sesión muestra carga, restaura el botón tras error y no revive sesión cerrada', async (t) => {
  const { doc, win } = page(t, '<form id="login-form"><input name="email" value="a@test.invalid"><button type="submit">Iniciar sesión</button></form><p id="account-feedback"></p>');
  const saved = { localStorage: globalThis.localStorage, FormData: globalThis.FormData };
  globalThis.localStorage = win.localStorage; globalThis.FormData = win.FormData;
  t.after(() => { globalThis.localStorage = saved.localStorage; globalThis.FormData = saved.FormData; });
  const requests = [];
  const account = initAccount(doc, win.location.origin, () => {}, () => new Promise((resolve) => requests.push(resolve)));
  const form = doc.querySelector('form'); const button = form.querySelector('button');
  form.dispatchEvent(new win.Event('submit', { cancelable: true }));
  assert.equal(button.disabled, true); assert.equal(form.getAttribute('aria-busy'), 'true');
  assert.match(button.textContent, /Iniciando/);
  requests[0]({ ok: false, json: async () => ({ message: 'Credenciales incorrectas.' }) }); await settle();
  assert.equal(button.disabled, false); assert.equal(button.textContent, 'Iniciar sesión');
  assert.equal(doc.querySelector('#account-feedback').getAttribute('data-status'), 'error');
  form.dispatchEvent(new win.Event('submit', { cancelable: true }));
  account.logout();
  assert.equal(button.disabled, false); assert.equal(form.getAttribute('aria-busy'), 'false');
  requests[1](ok({ accessToken: 'old', user: { name: 'Anterior', role: 'GUEST' }, expiresAt: '2099-01-01T00:00:00Z' })); await settle();
  assert.equal(account.getSession(), null);
  assert.equal(win.localStorage.getItem('marketplace-session'), null);
});
