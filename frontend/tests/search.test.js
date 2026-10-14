import test from 'node:test';
import assert from 'node:assert/strict';
import { validateSearch } from '../src/search.js';

test('rechaza una búsqueda sin ciudad', () => {
  assert.match(validateSearch({ city: ' ', checkin: '2026-11-01', checkout: '2026-11-03', guests: '2' }), /ciudad/i);
});

test('rechaza una salida igual o anterior a la llegada', () => {
  assert.match(validateSearch({ city: 'Bogotá', checkin: '2026-11-03', checkout: '2026-11-03', guests: '2' }), /posterior/i);
});

test('acepta una búsqueda válida', () => {
  assert.equal(validateSearch({ city: 'Bogotá', checkin: '2026-11-01', checkout: '2026-11-03', guests: '2' }), '');
});
