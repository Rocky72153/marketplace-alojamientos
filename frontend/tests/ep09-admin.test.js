import test from 'node:test';
import assert from 'node:assert/strict';
import { initAdministration } from '../src/ep09-admin.js';

class Element {
  constructor(tagName) { this.tagName = tagName; this.children = []; this.listeners = {}; this.textContent = ''; this.value = ''; this.hidden = false; }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute() {}
  addEventListener(name, callback) { this.listeners[name] = callback; }
  async dispatch(name) { return this.listeners[name]?.({ preventDefault() {} }); }
}
const doc = { createElement: (name) => new Element(name) };
const ok = (body) => ({ ok: true, json: async () => body });
const page = (items) => ({ content: items, totalElements: items.length, totalPages: 1 });
function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) { const match = find(child, predicate); if (match) return match; }
  return null;
}
function byText(node, text) { return find(node, (element) => element.textContent === text); }
const dashboard = { generatedAt: '2026-11-11', timezone: 'America/Bogota', users: 2, activeListings: 1,
  estimatedRevenue: 120000, currency: 'COP', bookingsByStatus: { CONFIRMED: 1 },
  topCities: [{ city: 'Bogotá', bookings: 1 }], definitions: { estimatedRevenue: 'Cobros simulados' } };
function backend(requests) {
  return async (url, options = {}) => {
    requests.push({ path: url.pathname, query: url.searchParams, options });
    if (url.pathname === '/api/admin/dashboard') return ok(dashboard);
    if (url.pathname === '/api/admin/users') return ok(page([{ id: 'u1', name: 'Ana', email: 'a@example.test', role: 'HOST', accountState: 'ACTIVE' }]));
    if (url.pathname === '/api/admin/listings') return ok(page([{ id: 'l1', title: 'Casa', city: 'Bogotá', status: 'ACTIVE' }]));
    if (url.pathname === '/api/admin/audit') return ok(page([{ eventType: 'ACCOUNT_RESTRICTED', actorId: 'a1', reason: 'Motivo', correlationId: 'solicitud-123' }]));
    if (url.pathname === '/api/admin/reservations/manual-review') return ok([{ id: 'b1', listingTitle: 'Casa', checkIn: '2026-12-01', checkOut: '2026-12-03', status: 'CONFIRMED' }]);
    if (url.pathname === '/api/admin/reports') return { ok: true, blob: async () => ({ file: true }) };
    return ok({});
  };
}

test('muestra indicadores y gestiones con autorización administrativa', async () => {
  const requests = [];
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc, backend(requests));
  await panel.refresh();
  assert.ok(byText(panel.section, 'Usuarios'));
  assert.ok(byText(panel.section, 'Bogotá: 1 reserva(s)'));
  assert.ok(byText(panel.section, 'estimatedRevenue: Cobros simulados'));
  assert.ok(byText(panel.section, 'Ana'));
  assert.ok(byText(panel.section, 'Reserva: b1'));
  assert.ok(byText(panel.section, 'Referencia de solicitud: solicitud-123'));
  assert.ok(requests.every((r) => r.options.headers.Authorization === 'Bearer admin'));
});

test('restringir cuenta y bloquear publicación requiere motivo y envía estado correcto', async () => {
  const requests = [];
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc, backend(requests));
  await panel.refresh();
  const user = byText(panel.section, 'Ana');
  const userCard = find(panel.section, (node) => node.children.includes(user));
  const form = find(userCard, (node) => node.tagName === 'form');
  await form.dispatch('submit');
  assert.ok(byText(form, 'Indica un motivo.'));
  find(form, (node) => node.tagName === 'textarea').value = ' Incumplimiento ';
  const beforeUserAudit = requests.filter((r) => r.path === '/api/admin/audit').length;
  await form.dispatch('submit');
  const restriction = requests.find((r) => r.path.endsWith('/users/u1/restrict'));
  assert.deepEqual(JSON.parse(restriction.options.body), { reason: 'Incumplimiento' });
  assert.equal(requests.filter((r) => r.path === '/api/admin/audit').length, beforeUserAudit + 1);
  const listingTitle = byText(panel.section, 'Casa');
  const listingCard = find(panel.section, (node) => node.children.includes(listingTitle));
  const listingForm = find(listingCard, (node) => node.tagName === 'form');
  find(listingForm, (node) => node.tagName === 'textarea').value = 'Información incorrecta';
  const beforeListingAudit = requests.filter((r) => r.path === '/api/admin/audit').length;
  await listingForm.dispatch('submit');
  const listingRequest = requests.find((r) => r.path.endsWith('/listings/l1/state'));
  assert.equal(listingRequest.options.method, 'PATCH');
  assert.deepEqual(JSON.parse(listingRequest.options.body), { status: 'BLOCKED', reason: 'Información incorrecta' });
  assert.equal(requests.filter((r) => r.path === '/api/admin/audit').length, beforeListingAudit + 1);
});

test('aplica fechas al dashboard y exporta formatos con las mismas fechas', async () => {
  const requests = []; const downloads = [];
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc, backend(requests),
    (blob, filename) => downloads.push({ blob, filename }));
  await panel.refresh();
  const filter = find(panel.section, (node) => node.className === 'admin-filters');
  const start = find(filter, (node) => node.name === 'from'); const end = find(filter, (node) => node.name === 'to');
  start.value = '2026-11-01'; end.value = '2026-11-15';
  await byText(panel.section, 'Descargar Excel').dispatch('click');
  assert.ok(byText(panel.section, 'Consulta los indicadores con las fechas seleccionadas antes de exportar.'));
  assert.equal(requests.some((r) => r.path === '/api/admin/reports'), false);
  await filter.dispatch('submit');
  await new Promise((resolve) => setImmediate(resolve));
  await byText(panel.section, 'Descargar Excel').dispatch('click');
  await new Promise((resolve) => setImmediate(resolve));
  const report = requests.find((r) => r.path === '/api/admin/reports');
  assert.equal(report.query.get('from'), '2026-11-01');
  assert.equal(report.query.get('to'), '2026-11-15');
  assert.equal(report.query.get('format'), 'XLSX');
  assert.equal(downloads[0].filename, 'reporte-marketplace.xlsx');
  const prior = requests.length; start.value = '2026-12-01';
  await filter.dispatch('submit');
  assert.equal(requests.length, prior);
  assert.ok(byText(panel.section, 'La fecha inicial debe ser anterior o igual a la final.'));
});

test('una cuenta desactivada no ofrece una restricción que el servidor rechazaría', async () => {
  const base = backend([]);
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc,
    (url, options) => url.pathname === '/api/admin/users'
      ? ok(page([{ id: 'u2', name: 'Luisa', email: 'l@example.test', role: 'HOST', accountState: 'DEACTIVATED' }]))
      : base(url, options));
  await panel.refresh();
  const label = byText(panel.section, 'Luisa');
  const card = find(panel.section, (node) => node.children.includes(label));
  assert.equal(byText(card, 'Restringir cuenta'), null);
});

test('paginación descarta respuestas que llegan fuera de orden', async () => {
  let resolveOld;
  const old = new Promise((resolve) => { resolveOld = resolve; });
  const base = backend([]);
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc,
    (url, options) => {
      if (url.pathname !== '/api/admin/users') return base(url, options);
      const number = Number(url.searchParams.get('page'));
      if (number === 0) return ok({ content: [{ id: 'u0', name: 'Página 0', accountState: 'RESTRICTED' }], totalPages: 3 });
      if (number === 1) return ok({ content: [{ id: 'u1', name: 'Página 1', accountState: 'RESTRICTED' }], totalPages: 3 });
      return old;
    });
  await panel.refresh();
  await byText(panel.section, 'Siguiente').dispatch('click');
  await new Promise((resolve) => setImmediate(resolve));
  await byText(panel.section, 'Siguiente').dispatch('click');
  await byText(panel.section, 'Anterior').dispatch('click');
  await new Promise((resolve) => setImmediate(resolve));
  resolveOld(ok({ content: [{ id: 'u2', name: 'Respuesta obsoleta', accountState: 'RESTRICTED' }], totalPages: 3 }));
  await new Promise((resolve) => setImmediate(resolve));
  assert.ok(byText(panel.section, 'Página 0'));
  assert.equal(byText(panel.section, 'Respuesta obsoleta'), null);
});

test('cerrar sesión elimina información y descarta respuestas tardías', async () => {
  let token = 'admin'; let resolveDashboard;
  const deferred = new Promise((resolve) => { resolveDashboard = resolve; });
  const handler = backend([]);
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => token, doc,
    (url, options) => url.pathname === '/api/admin/dashboard' ? deferred : handler(url, options));
  const loading = panel.refresh();
  await new Promise((resolve) => setImmediate(resolve));
  token = null; panel.clear(); resolveDashboard(ok(dashboard)); await loading;
  assert.equal(panel.section.hidden, true);
  assert.equal(byText(panel.section, 'Ana'), null);
  assert.equal(byText(panel.section, 'Bogotá: 1 reserva(s)'), null);
});

test('muestra un error de autorización sin presentar datos', async () => {
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'guest', doc,
    async () => ({ ok: false, json: async () => ({ message: 'Acceso denegado' }) }));
  await panel.refresh();
  assert.ok(byText(panel.section, 'Acceso denegado'));
  assert.equal(byText(panel.section, 'Ana'), null);
});

test('el tablero descarta una consulta de fechas reemplazada por otra más reciente', async () => {
  let resolveOld; let dashboardCalls = 0;
  const old = new Promise((resolve) => { resolveOld = resolve; });
  const handler = backend([]);
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc,
    (url, options) => {
      if (url.pathname !== '/api/admin/dashboard') return handler(url, options);
      dashboardCalls += 1;
      return dashboardCalls === 1 ? old : ok({ ...dashboard, users: 9 });
    });
  const first = panel.refresh();
  const filter = find(panel.section, (node) => node.className === 'admin-filters');
  await filter.dispatch('submit');
  await new Promise((resolve) => setImmediate(resolve));
  resolveOld(ok({ ...dashboard, users: 2 })); await first;
  assert.ok(byText(panel.section, '9'));
  assert.equal(byText(panel.section, '2'), null);
});

test('permite registrar revisión manual y rechaza exportación con fechas inválidas', async () => {
  const requests = [];
  const panel = initAdministration(new Element('div'), 'http://localhost:8080', () => 'admin', doc, backend(requests));
  await panel.refresh();
  const label = byText(panel.section, 'Reserva: b1');
  const card = find(panel.section, (node) => node.children.includes(label));
  const form = find(card, (node) => node.tagName === 'form');
  find(form, (node) => node.tagName === 'textarea').value = 'Revisada con el anfitrión';
  await form.dispatch('submit');
  const resolve = requests.find((r) => r.path.endsWith('/reservations/b1/resolve-review'));
  assert.deepEqual(JSON.parse(resolve.options.body), { reason: 'Revisada con el anfitrión' });
  const filter = find(panel.section, (node) => node.className === 'admin-filters');
  find(filter, (node) => node.name === 'from').value = '2026-12-01';
  find(filter, (node) => node.name === 'to').value = '2026-11-01';
  await byText(panel.section, 'Descargar PDF').dispatch('click');
  assert.equal(requests.some((r) => r.path === '/api/admin/reports'), false);
});
