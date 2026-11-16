import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const listingId = 'a4887b19-653a-4c03-b2d8-7717ebcd3205';
const listing = (title = 'Casa segura') => ({ id: listingId, title, city: 'Bogotá', type: 'APARTMENT', capacity: 4,
  pricePerNight: 180000, amenityIds: ['WIFI', 'POOL'] });
const ok = (body) => ({ ok: true, json: async () => body });
const flush = () => new Promise(setImmediate);
let instance = 0;

async function fixture(search, { apiOrigin = 'http://localhost:8080' } = {}) {
  const dom = new JSDOM(html, { url: 'https://marketplace.example.test/' });
  const { window } = dom;
  const previous = Object.fromEntries(['window', 'document', 'location', 'localStorage', 'FormData', 'fetch']
    .map((key) => [key, globalThis[key]]));
  window.document.querySelector('meta[name="api-base-url"]').content = apiOrigin;
  window.scrollTo = () => {};
  Object.assign(globalThis, { window, document: window.document, location: window.location,
    localStorage: window.localStorage, FormData: window.FormData });
  globalThis.fetch = async (input, options) => {
    const url = new URL(String(input));
    if (url.pathname === '/api/listings/cities') return ok([]);
    if (url.pathname === '/api/listings/suggestions') return ok({ items: [] });
    if (url.pathname === '/api/listings') return search(url, options);
    throw new Error(`Solicitud inesperada: ${url.pathname}`);
  };
  try {
    await import(`../src/main.js?jsdom-main-${++instance}`);
  } catch (error) {
    Object.assign(globalThis, previous);
    dom.window.close();
    throw error;
  }
  const doc = window.document;
  const form = doc.querySelector('#search-form');
  const field = (name) => form.querySelector(`[name="${name}"]`);
  const submit = () => form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
  function setSearch(city = 'Bogotá') {
    field('city').value = city;
    field('checkin').value = '2099-12-03';
    field('checkout').value = '2099-12-05';
    field('guests').value = '2';
  }
  return { window, doc, form, field, submit, setSearch,
    feedback: doc.querySelector('#search-feedback'), results: doc.querySelector('#search-results'),
    cleanup() { dom.window.close(); Object.assign(globalThis, previous); } };
}

test('conecta validación, filtros, resultados, vacío y error con el formulario real', async () => {
  const calls = [];
  const app = await fixture(async (url) => {
    calls.push(url);
    if (url.searchParams.get('city') === 'Error') return { ok: false };
    if (url.searchParams.get('city') === 'Sin resultados') return ok({ total: 0, items: [] });
    return ok({ total: 1, items: [listing()] });
  });
  try {
    app.setSearch('');
    app.field('checkin').value = '2099-12-05';
    app.field('checkout').value = '2099-12-04';
    app.field('checkin').dispatchEvent(new app.window.Event('change', { bubbles: true }));
    assert.equal(app.field('checkout').min, '2099-12-05');
    assert.equal(app.field('checkout').value, '');
    app.setSearch(''); app.submit();
    assert.match(app.feedback.textContent, /ciudad/i);
    assert.equal(calls.length, 0);
    assert.equal(app.results.querySelectorAll('.listing-card').length, 0);

    app.setSearch();
    app.field('type').value = 'APARTMENT';
    app.field('minPrice').value = '100000';
    app.field('maxPrice').value = '200000';
    for (const value of ['WIFI', 'POOL']) app.form.querySelector(`[name="amenityIds"][value="${value}"]`).checked = true;
    app.submit(); await flush();
    const params = calls[0].searchParams;
    for (const [key, expected] of Object.entries({ city: 'Bogotá', checkIn: '2099-12-03', checkOut: '2099-12-05',
      guests: '2', type: 'APARTMENT', minPrice: '100000', maxPrice: '200000' })) assert.equal(params.get(key), expected);
    assert.deepEqual(params.getAll('amenityIds'), ['WIFI', 'POOL']);
    assert.match(app.feedback.textContent, /1 alojamiento encontrado/i);
    const cards = app.results.querySelectorAll('.listing-card');
    assert.equal(cards.length, 1);
    assert.match(cards[0].querySelector('h3').textContent, /Casa segura/);
    assert.match(cards[0].textContent, /Bogotá/);
    const link = cards[0].querySelector('a.listing-detail-link');
    assert.equal(link.textContent, 'Ver alojamiento');
    const detail = new URL(link.href);
    assert.equal(detail.searchParams.get('listing'), listingId);
    for (const [key, expected] of Object.entries({ city: 'Bogotá', checkin: '2099-12-03', checkout: '2099-12-05',
      guests: '2', type: 'APARTMENT', minPrice: '100000', maxPrice: '200000' })) assert.equal(detail.searchParams.get(key), expected);
    assert.deepEqual(detail.searchParams.getAll('amenityIds'), ['WIFI', 'POOL']);

    app.field('city').value = 'Sin resultados'; app.submit(); await flush();
    assert.match(app.feedback.textContent, /no encontramos alojamientos/i);
    assert.equal(app.results.querySelectorAll('.listing-card').length, 0);
    assert.match(app.results.querySelector('.empty-state').textContent, /cambiar criterios/i);
    app.field('city').value = 'Error'; app.submit(); await flush();
    assert.match(app.feedback.textContent, /no fue posible consultar/i);
    assert.match(app.results.querySelector('.empty-state').textContent, /reintentar búsqueda/i);
  } finally { app.cleanup(); }
});

test('ignora respuestas antiguas tras una búsqueda nueva o un error de validación', async () => {
  const pending = [];
  const app = await fixture(() => new Promise((resolve) => pending.push(resolve)));
  try {
    app.setSearch(); app.submit();
    assert.equal(app.results.getAttribute('aria-busy'), 'true');
    app.field('city').value = 'Medellín'; app.submit();
    pending[1](ok({ total: 1, items: [listing('Resultado nuevo')] })); await flush();
    pending[0](ok({ total: 1, items: [listing('Resultado anterior')] })); await flush();
    assert.match(app.results.querySelector('.listing-card h3').textContent, /Resultado nuevo/);
    assert.equal(app.results.querySelectorAll('.listing-card').length, 1);
    app.submit();
    app.field('city').value = ''; app.submit();
    pending[2](ok({ total: 1, items: [listing('Resultado caducado')] })); await flush();
    assert.equal(app.results.querySelectorAll('.listing-card').length, 0);
    assert.match(app.feedback.textContent, /ciudad/i);
    assert.equal(app.results.getAttribute('aria-busy'), 'false');
  } finally { app.cleanup(); }
});

test('usa el origen del sitio para consultar la API cuando no se configura otro', async () => {
  const calls = [];
  const app = await fixture(async (url) => { calls.push(url); return ok({ items: [], total: 0 }); }, { apiOrigin: '' });
  try {
    app.setSearch(); app.submit(); await flush();
    assert.equal(calls.length, 1);
    assert.equal(calls[0].origin, 'https://marketplace.example.test');
  } finally { app.cleanup(); }
});
