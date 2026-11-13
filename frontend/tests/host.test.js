import test from 'node:test';
import assert from 'node:assert/strict';
import { initHostWorkspace } from '../src/host.js';

class Element {
  constructor(tag) { this.tagName = tag; this.children = []; this.listeners = {}; this.textContent = ''; this.value = ''; this.hidden = false; }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute() {}
  addEventListener(name, callback) { this.listeners[name] = callback; }
  async dispatch(name) { return this.listeners[name]?.({ preventDefault() {} }); }
}
const doc = { createElement: (tag) => new Element(tag) };
const ok = (value) => ({ ok: true, json: async () => value });
const session = { user: { role: 'HOST' }, accessToken: 'host-token' };
function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) { const result = find(child, predicate); if (result) return result; }
  return null;
}
const byText = (node, text) => find(node, (child) => child.textContent === text);
const field = (node, name) => find(node, (child) => child.name === name);
const listing = { id: 'l1', title: 'Casa', description: 'Una casa', city: 'Bogotá', location: 'Centro',
  type: 'HOTEL', capacity: 3, pricePerNight: 100000, status: 'DRAFT', amenityIds: ['WIFI', 'BREAKFAST'],
  imageReferences: ['https://example.test/casa.jpg'], extraOptions: [{ id: 'e1', name: 'Mascota', unitPrice: 10000 }],
  cancellationPolicy: { freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 } };

test('el anfitrión consulta únicamente su panel y edita los datos completos sin perder extras', async () => {
  const calls = [];
  const fetch = async (url, options = {}) => {
    calls.push({ path: url.pathname, options });
    if (options.method === 'PATCH') return ok(listing);
    return ok({ content: [listing], totalPages: 1 });
  };
  const panel = initHostWorkspace(new Element('div'), 'http://localhost:8080', () => session, doc, fetch);
  await panel.refresh();
  assert.ok(byText(panel.section, 'Casa'));
  assert.equal(calls[0].path, '/api/host/listings');
  assert.equal(calls[0].options.headers.Authorization, 'Bearer host-token');
  await byText(panel.section, 'Editar').dispatch('click');
  field(panel.section, 'title').value = 'Casa renovada';
  const form = find(panel.section, (node) => node.tagName === 'form');
  await form.dispatch('submit');
  const update = calls.find((call) => call.options.method === 'PATCH');
  assert.equal(update.path, '/api/listings/l1');
  const body = JSON.parse(update.options.body);
  assert.equal(body.title, 'Casa renovada');
  assert.equal(body.type, 'HOTEL');
  assert.deepEqual(body.amenityIds.sort(), ['BREAKFAST', 'WIFI']);
  assert.deepEqual(body.extraOptions, [{ id: 'e1', name: 'Mascota', unitPrice: 10000 }]);
  assert.deepEqual(body.imageReferences, ['https://example.test/casa.jpg']);
  assert.equal(body.cancellationPolicy.freeRefundDays, 7);
});

test('crea un alojamiento como borrador con condiciones comerciales', async () => {
  const calls = [];
  const fetch = async (url, options = {}) => {
    calls.push({ path: url.pathname, options });
    return options.method === 'POST' ? ok({ id: 'new' }) : ok({ content: [], totalPages: 0 });
  };
  const panel = initHostWorkspace(new Element('div'), 'http://localhost:8080', () => session, doc, fetch);
  await panel.refresh();
  await byText(panel.section, 'Crear alojamiento').dispatch('click');
  field(panel.section, 'title').value = 'Nueva casa';
  field(panel.section, 'description').value = 'Cerca del centro';
  field(panel.section, 'city').value = 'Cali';
  field(panel.section, 'location').value = 'San Antonio';
  field(panel.section, 'pricePerNight').value = '120000';
  const form = find(panel.section, (node) => node.tagName === 'form');
  await form.dispatch('submit');
  const create = calls.find((call) => call.options.method === 'POST');
  assert.equal(create.path, '/api/listings');
  const body = JSON.parse(create.options.body);
  assert.equal(body.title, 'Nueva casa');
  assert.equal(body.pricePerNight, 120000);
  assert.deepEqual(body.cancellationPolicy, { freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 });
  assert.ok(byText(panel.section, 'Alojamiento creado como borrador.'));
});

test('activa el alojamiento y conserva períodos existentes al añadir disponibilidad', async () => {
  const calls = [];
  const periods = [{ id: 'a1', startDate: '2027-01-01', endDate: '2027-01-05', type: 'AVAILABLE' },
    { id: 'r1', startDate: '2027-01-10', endDate: '2027-01-12', type: 'RESERVED' }];
  const fetch = async (url, options = {}) => {
    calls.push({ path: url.pathname, options });
    if (url.pathname.endsWith('/availability') && !options.method) return ok(periods);
    if (options.method) return ok({});
    return ok({ content: [listing], totalPages: 1 });
  };
  const panel = initHostWorkspace(new Element('div'), 'http://localhost:8080', () => session, doc, fetch);
  await panel.refresh();
  await byText(panel.section, 'Activar').dispatch('click');
  assert.deepEqual(JSON.parse(calls.find((call) => call.path.endsWith('/state')).options.body), { status: 'ACTIVE' });
  await byText(panel.section, 'Administrar disponibilidad').dispatch('click');
  field(panel.section, 'start').value = '2027-02-01';
  field(panel.section, 'end').value = '2027-02-05';
  const dateInput = field(panel.section, 'start');
  const form = find(panel.section, (node) => node.tagName === 'form' && find(node, (child) => child === dateInput));
  await form.dispatch('submit');
  const update = calls.find((call) => call.options.method === 'PUT');
  assert.deepEqual(JSON.parse(update.options.body).periods, [
    { startDate: '2027-01-01', endDate: '2027-01-05', type: 'AVAILABLE' },
    { startDate: '2027-02-01', endDate: '2027-02-05', type: 'AVAILABLE' },
  ]);
});

test('no muestra respuestas tardías después de cerrar sesión ni carga datos de otros roles', async () => {
  let active = session;
  let resolve;
  const panel = initHostWorkspace(new Element('div'), 'http://localhost:8080', () => active, doc,
    () => new Promise((done) => { resolve = done; }));
  const pending = panel.refresh();
  active = null; panel.clear();
  resolve(ok({ content: [listing], totalPages: 1 }));
  await pending;
  assert.equal(byText(panel.section, 'Casa'), null);
  active = { user: { role: 'GUEST' }, accessToken: 'guest-token' };
  await panel.refresh();
  assert.equal(byText(panel.section, 'Casa'), null);
});
