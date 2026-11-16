import test from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { renderHomeSuggestions } from '../src/home-suggestions.js';

function fixture(t) {
  const dom = new JSDOM('<main id="suggestions"></main>', { url: 'http://marketplace.test/' });
  t.after(() => dom.window.close());
  return { doc: dom.window.document, root: dom.window.document.querySelector('#suggestions') };
}
const response = (items) => ({ ok: true, json: async () => ({ items }) });
const sample = (id, title = 'Casa de prueba') => ({ id, title, city: 'Bogotá', type: 'HOUSE',
  capacity: 3, pricePerNight: 100000 });

test('muestra ocho tarjetas públicas sin solicitar sesión ni todo el catálogo', async (t) => {
  const { root, doc } = fixture(t);
  let requested;
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async (url, options) => {
    requested = { url, options };
    return response(Array.from({ length: 12 }, (_, index) => sample(`id/${index}`)));
  });
  assert.equal(requested.url.pathname, '/api/listings/suggestions');
  assert.equal(requested.url.search, '');
  assert.deepEqual(requested.options.headers, { Accept: 'application/json' });
  assert.equal(root.querySelector('h2').textContent, 'Descubre alojamientos');
  assert.equal(root.querySelectorAll('.listing-card').length, 8);
  const card = root.querySelector('.listing-card');
  assert.equal(card.querySelector('.listing-card-location').textContent, 'Bogotá');
  assert.equal(card.querySelector('.listing-card-type').textContent, 'Casa');
  assert.equal(card.querySelector('.listing-card-capacity').textContent, 'Hasta 3 huéspedes');
  assert.equal(card.querySelector('.listing-detail-link').getAttribute('href'), '/?listing=id%2F0');
  assert.match(card.querySelector('.listing-photo-placeholder').textContent, /Ilustración.*Sin fotos publicadas/);
});

test('presenta estado vacío o error recuperable sin bloquear la búsqueda', async (t) => {
  const { root, doc } = fixture(t);
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async () => response([]));
  assert.equal(root.querySelector('.feedback').textContent, 'Aún no hay alojamientos sugeridos.');
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async () => { throw new Error('red'); });
  assert.match(root.querySelector('.feedback').textContent, /Puedes buscar alojamientos/);
  assert.equal(root.querySelectorAll('.listing-card').length, 0);
});

test('ignora respuestas tardías y mantiene títulos como texto seguro', async (t) => {
  const { root, doc } = fixture(t);
  let resolveFirst;
  const pending = renderHomeSuggestions(root, 'http://localhost:8080', doc,
    () => new Promise((resolve) => { resolveFirst = resolve; }));
  await renderHomeSuggestions(root, 'http://localhost:8080', doc,
    async () => response([sample('new', '<img src=x onerror=alert(1)>')]));
  resolveFirst(response([sample('old', 'Respuesta antigua')]));
  await pending;
  assert.equal(root.querySelectorAll('.listing-card').length, 1);
  assert.equal(root.querySelector('h3').textContent, '<img src=x onerror=alert(1)>');
  assert.equal(root.querySelector('h3 img'), null);
  assert.equal(root.querySelector('.listing-detail-link').getAttribute('href'), '/?listing=new');
});
