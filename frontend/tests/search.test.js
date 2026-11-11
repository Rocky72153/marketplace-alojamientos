import test from 'node:test';
import assert from 'node:assert/strict';
import { buildCitySuggestionsUrl, buildSearchUrl, fetchCitySuggestions, validateSearch } from '../src/search.js';

test('rechaza una búsqueda sin ciudad', () => {
  assert.match(validateSearch({ city: ' ', checkin: '2026-11-01', checkout: '2026-11-03', guests: '2' }), /ciudad/i);
});

test('serializes valid search filters for the listings API', () => {
  const url = new URL(buildSearchUrl({ city: ' Bogotá ', checkin: '2026-12-01', checkout: '2026-12-05', guests: '2', type: 'HOUSE', minPrice: '50000', maxPrice: '200000', amenityIds: ['WIFI', 'POOL'] }));
  assert.equal(url.pathname, '/api/listings');
  assert.equal(url.searchParams.get('city'), 'Bogotá');
  assert.equal(url.searchParams.get('checkIn'), '2026-12-01');
  assert.equal(url.searchParams.get('checkOut'), '2026-12-05');
  assert.equal(url.searchParams.get('guests'), '2');
  assert.equal(url.searchParams.get('type'), 'HOUSE');
  assert.equal(url.searchParams.get('minPrice'), '50000');
  assert.deepEqual(url.searchParams.getAll('amenityIds'), ['WIFI', 'POOL']);
});

test('rechaza un rango de precios invertido', () => {
  assert.match(validateSearch({ city: 'Bogotá', checkin: '2026-11-01', checkout: '2026-11-03', guests: '2', minPrice: '200', maxPrice: '100' }), /precio/i);
});

test('rechaza una salida igual o anterior a la llegada', () => {
  assert.match(validateSearch({ city: 'Bogotá', checkin: '2026-11-03', checkout: '2026-11-03', guests: '2' }), /posterior/i);
});

test('acepta una búsqueda válida', () => {
  assert.equal(validateSearch({ city: 'Bogotá', checkin: '2026-11-01', checkout: '2026-11-03', guests: '2' }), '');
});

test('construye la consulta de sugerencias y deja cargar destinos iniciales sin filtro', () => {
  const filtered = new URL(buildCitySuggestionsUrl('  Bogotá  ', 'http://localhost:8080/'));
  assert.equal(filtered.pathname, '/api/listings/cities');
  assert.equal(filtered.searchParams.get('query'), 'Bogotá');
  assert.equal(buildCitySuggestionsUrl('', 'http://localhost:8080'), 'http://localhost:8080/api/listings/cities');
});

test('consulta sugerencias públicas de ciudad', async () => {
  const previousFetch = globalThis.fetch;
  let requestedUrl = '';
  globalThis.fetch = async (url, options) => {
    requestedUrl = String(url);
    assert.equal(options.headers.Accept, 'application/json');
    return { ok: true, async json() { return ['Bogotá D.C., Bogotá D.C.']; } };
  };
  try {
    assert.deepEqual(await fetchCitySuggestions('Bogota'), ['Bogotá D.C., Bogotá D.C.']);
    assert.equal(new URL(requestedUrl).searchParams.get('query'), 'Bogota');
  } finally {
    globalThis.fetch = previousFetch;
  }
});
