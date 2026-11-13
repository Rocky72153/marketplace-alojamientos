import test from 'node:test';
import assert from 'node:assert/strict';
import { renderHomeSuggestions } from '../src/home-suggestions.js';

class Element {
  constructor(tag) { this.tagName = tag; this.children = []; this.textContent = ''; this.className = ''; this.attributes = {}; }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute(name, value) { this.attributes[name] = value; }
}
const doc = { createElement: (tag) => new Element(tag) };
const response = (items) => ({ ok: true, json: async () => ({ items }) });
const sample = (id, title = 'Casa de prueba') => ({ id, title, city: 'Bogotá', type: 'HOUSE',
  capacity: 3, pricePerNight: 100000 });

test('muestra tarjetas públicas sin solicitar una sesión ni todo el catálogo', async () => {
  const root = new Element('div');
  let requested;
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async (url, options) => {
    requested = { url, options };
    return response(Array.from({ length: 12 }, (_, index) => sample(`id/${index}`)));
  });
  assert.equal(requested.url.pathname, '/api/listings/suggestions');
  assert.equal(requested.url.search, '');
  assert.deepEqual(requested.options.headers, { Accept: 'application/json' });
  assert.equal(root.children[0].textContent, 'Descubre alojamientos');
  assert.equal(root.children[2].children.length, 8);
  assert.equal(root.children[2].children[0].children[1].textContent, 'Bogotá · Casa · Hasta 3 huéspedes');
  assert.equal(root.children[2].children[0].children[3].href, '/?listing=id%2F0');
});

test('presenta estado vacío o un error recuperable sin bloquear la búsqueda', async () => {
  const root = new Element('div');
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async () => response([]));
  assert.equal(root.children[1].textContent, 'Aún no hay alojamientos sugeridos.');
  await renderHomeSuggestions(root, 'http://localhost:8080', doc, async () => { throw new Error('red'); });
  assert.match(root.children[1].textContent, /Puedes buscar alojamientos/);
  assert.equal(root.children[2].children.length, 0);
});

test('ignora respuestas tardías y usa texto seguro para títulos', async () => {
  const root = new Element('div');
  let resolveFirst;
  const pending = renderHomeSuggestions(root, 'http://localhost:8080', doc,
    () => new Promise((resolve) => { resolveFirst = resolve; }));
  await renderHomeSuggestions(root, 'http://localhost:8080', doc,
    async () => response([sample('new', '<img src=x onerror=alert(1)>')]));
  resolveFirst(response([sample('old', 'Respuesta antigua')]));
  await pending;
  assert.equal(root.children[2].children.length, 1);
  assert.equal(root.children[2].children[0].children[0].textContent, '<img src=x onerror=alert(1)>');
  assert.equal(root.children[2].children[0].children[3].href, '/?listing=new');
});
