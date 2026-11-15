import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { JSDOM } from 'jsdom';
import { initSearchResults } from '../src/search-results.js';
import { buildSearchDetailUrl, buildSearchReturnUrl, readSearchFilters } from '../src/search.js';
import { initNavigation } from '../src/navigation.js';

const html = readFileSync(new URL('../index.html', import.meta.url), 'utf8');
const filters = { city: 'Bogotá', checkin: '2026-12-10', checkout: '2026-12-12', guests: '3',
  type: 'HOUSE', minPrice: '50000', maxPrice: '300000', amenityIds: ['WIFI', 'POOL'] };
const listing = (id) => ({ id, title: `Casa ${id}`, city: 'Bogotá', type: 'HOUSE', capacity: 4, pricePerNight: 120000 });
const response = (items) => ({ ok: true, json: async () => ({ items, total: items.length }) });
const settle = () => new Promise(setImmediate);

function fixture(url = 'http://marketplace.test/') {
  const dom = new JSDOM(html, { url });
  return { dom, doc: dom.window.document, win: dom.window };
}

test('detalle y retorno conservan fechas, huéspedes, filtros y página sin parámetros de pago', () => {
  const detail = new URL(buildSearchDetailUrl('listing/id', filters, 2), 'http://marketplace.test');
  assert.equal(detail.searchParams.get('listing'), 'listing/id');
  assert.deepEqual(readSearchFilters(detail.searchParams), filters);
  assert.equal(detail.searchParams.get('searchPage'), '2');
  detail.searchParams.set('stripe_checkout', 'private-attempt');
  const back = new URL(buildSearchReturnUrl(detail.searchParams), detail.origin);
  assert.equal(back.hash, '#explorar');
  assert.equal(back.searchParams.has('listing'), false);
  assert.equal(back.searchParams.has('stripe_checkout'), false);
  assert.deepEqual(readSearchFilters(back.searchParams), filters);
  assert.equal(back.searchParams.get('searchPage'), '2');
  assert.equal(buildSearchReturnUrl('listing=from-suggestions'), '/#explorar');
});

test('formulario real busca y pagina toda la respuesta pública sin perder criterios al abrir ficha', async () => {
  const f = fixture();
  const requests = [];
  const controller = initSearchResults(f.doc, f.win.location.origin, { windowRef: f.win, pageSize: 2,
    fetchImpl: async (url) => { requests.push(new URL(url)); return response([listing('1'), listing('2'), listing('3')]); } });
  for (const key of ['city', 'checkin', 'checkout', 'guests', 'type', 'minPrice', 'maxPrice']) {
    f.doc.querySelector(`[name="${key}"]`).value = filters[key];
  }
  f.doc.querySelector('[name="amenityIds"][value="WIFI"]').checked = true;
  f.doc.querySelector('[name="amenityIds"][value="POOL"]').checked = true;
  f.doc.querySelector('#search-form').dispatchEvent(new f.win.Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  assert.equal(requests.length, 1);
  assert.equal(requests[0].searchParams.get('checkIn'), filters.checkin);
  assert.equal(requests[0].searchParams.has('page'), false);
  assert.equal(f.doc.querySelectorAll('#search-results .listing-card').length, 2);
  assert.match(f.doc.querySelector('#search-feedback').textContent, /3 alojamientos encontrados.*1–2/);
  f.doc.querySelector('.search-pagination button:last-child').click();
  assert.equal(requests.length, 1);
  assert.equal(f.doc.querySelectorAll('#search-results .listing-card').length, 1);
  assert.match(f.doc.querySelector('#search-feedback').textContent, /3–3/);
  const detail = new URL(f.doc.querySelector('#search-results .listing-detail-link').href);
  assert.equal(detail.searchParams.get('listing'), '3');
  assert.deepEqual(readSearchFilters(detail.searchParams), filters);
  assert.equal(detail.searchParams.get('searchPage'), '1');
  assert.equal(new URL(f.win.location.href).searchParams.get('searchPage'), '1');
  assert.equal(f.doc.querySelector('.search-pagination button:last-child').disabled, true);
  assert.ok(controller);
  f.dom.window.close();
});

test('volver a resultados restaura formulario y página; entrar a detalle difiere la búsqueda hasta volver', async () => {
  const detail = new URL(buildSearchDetailUrl('3', filters, 1), 'http://marketplace.test');
  const f = fixture(detail.href);
  let requests = 0;
  const controller = initSearchResults(f.doc, detail.origin, { windowRef: f.win, pageSize: 2,
    fetchImpl: async () => { requests += 1; return response([listing('1'), listing('2'), listing('3')]); } });
  assert.equal(requests, 0);
  assert.equal(f.doc.querySelector('#checkin').value, filters.checkin);
  assert.equal(f.doc.querySelector('#guests').value, filters.guests);
  assert.equal(f.doc.querySelector('[value="POOL"]').checked, true);
  controller.resume();
  await settle();
  assert.equal(requests, 1);
  assert.match(f.doc.querySelector('.search-pagination').textContent, /Página 2 de 2/);
  controller.resume();
  assert.equal(requests, 1);
  f.dom.window.close();
});

test('respuesta de búsqueda anterior no reemplaza la nueva ni un error de validación', async () => {
  const f = fixture();
  const pending = [];
  const controller = initSearchResults(f.doc, f.win.location.origin, { windowRef: f.win,
    fetchImpl: () => new Promise((resolve) => pending.push(resolve)) });
  const old = controller.search(filters);
  const fresh = controller.search({ ...filters, city: 'Medellín' });
  pending[1](response([listing('nuevo')])); await fresh;
  pending[0](response([listing('antiguo')])); await old;
  assert.match(f.doc.querySelector('#search-results').textContent, /nuevo/);
  const stale = controller.search(filters);
  await controller.search({ ...filters, city: '' });
  pending[2](response([listing('caducado')])); await stale;
  assert.equal(f.doc.querySelector('#search-results').children.length, 0);
  assert.match(f.doc.querySelector('#search-feedback').textContent, /ciudad/);
  f.dom.window.close();
});

test('una búsqueda lenta no cambia la cuenta abierta ni la devuelve al inicio al iniciar sesión', async (t) => {
  const f = fixture();
  t.after(() => f.dom.window.close());
  f.win.scrollTo = () => {};
  let session = null;
  let completeSearch;
  const navigation = initNavigation(f.doc, f.win, () => session);
  const controller = initSearchResults(f.doc, f.win.location.origin, { windowRef: f.win,
    fetchImpl: () => new Promise((resolve) => { completeSearch = resolve; }) });
  const pendingSearch = controller.search(filters);
  navigation.navigate('account');
  assert.equal(f.win.location.hash, '#mi-cuenta');
  completeSearch(response([listing('tardio')]));
  await pendingSearch;
  assert.equal(f.win.location.hash, '#mi-cuenta');
  assert.equal(f.doc.body.dataset.view, 'account');
  session = { accessToken: 'guest-token', user: { role: 'GUEST' } };
  navigation.syncSession();
  assert.equal(f.win.location.hash, '#mi-cuenta');
  assert.equal(f.doc.body.dataset.view, 'account');
  assert.equal(f.doc.querySelector('#view-account').hidden, false);
  assert.equal(f.doc.querySelector('#view-explore').hidden, true);
});
