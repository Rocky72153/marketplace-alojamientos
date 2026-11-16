import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';
import { initShell } from '../src/ui.js';
import { initSearchResults } from '../src/search-results.js';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const settle = () => new Promise(setImmediate);

test('mobile menu closes on navigation or Escape and skipping content preserves the current view', () => {
  const dom = new JSDOM(html, { url: 'http://marketplace.test/#administracion' });
  const { document: doc } = dom.window;
  initShell(doc, dom.window);
  const toggle = doc.querySelector('#menu-toggle');
  const nav = doc.querySelector('#primary-navigation');
  toggle.click();
  assert.equal(toggle.getAttribute('aria-expanded'), 'true');
  assert.equal(nav.hasAttribute('data-open'), true);
  doc.dispatchEvent(new dom.window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
  assert.equal(toggle.getAttribute('aria-expanded'), 'false');
  assert.equal(doc.activeElement, toggle);
  toggle.click();
  nav.querySelector('a').dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }));
  assert.equal(nav.hasAttribute('data-open'), false);
  doc.querySelector('.skip-link').click();
  assert.equal(doc.activeElement.id, 'main-content');
  assert.equal(dom.window.location.hash, '#administracion');
  doc.querySelector('[data-destination="Medellín"]').click();
  assert.equal(doc.querySelector('#city').value, 'Medellín');
  assert.equal(doc.activeElement.id, 'checkin');
  dom.window.close();
});

test('clearing filters preserves destination, dates and guests and refreshes the results and summary', async () => {
  const dom = new JSDOM(html, { url: 'http://marketplace.test/' });
  const doc = dom.window.document;
  const requests = [];
  const controller = initSearchResults(doc, dom.window.location.origin, { windowRef: dom.window,
    fetchImpl: async (url) => {
      requests.push(new URL(url));
      return { ok: true, json: async () => ({ items: [{ id: 'listing-1', title: 'Casa de prueba', city: 'Bogotá',
        type: 'HOUSE', capacity: 3, pricePerNight: 100000, amenityIds: ['WIFI'] }], total: 1 }) };
    } });
  for (const [name, value] of Object.entries({ city: 'Bogotá', checkin: '2026-12-10', checkout: '2026-12-12', guests: '3', type: 'HOUSE', minPrice: '50000' })) {
    doc.querySelector(`[name="${name}"]`).value = value;
  }
  doc.querySelector('[name="amenityIds"][value="WIFI"]').checked = true;
  doc.querySelector('#search-form').dispatchEvent(new dom.window.Event('change', { bubbles: true }));
  assert.equal(doc.querySelector('#active-filter-count').textContent, '3');
  await controller.search();
  assert.equal(doc.querySelector('#search-results-heading').hidden, false);
  assert.match(doc.querySelector('#search-summary').textContent, /Bogotá.*3 huéspedes/);
  doc.querySelector('#clear-search-filters').click();
  await settle();
  assert.equal(requests.length, 2);
  assert.equal(requests[1].searchParams.get('city'), 'Bogotá');
  assert.equal(requests[1].searchParams.get('checkIn'), '2026-12-10');
  assert.equal(requests[1].searchParams.get('guests'), '3');
  assert.equal(requests[1].searchParams.has('minPrice'), false);
  assert.equal(requests[1].searchParams.has('type'), false);
  assert.deepEqual(requests[1].searchParams.getAll('amenityIds'), []);
  assert.equal(doc.querySelector('#active-filter-count').hidden, true);
  assert.equal(doc.querySelector('#search-results').getAttribute('aria-busy'), 'false');
  assert.equal(doc.querySelectorAll('#search-results .listing-card').length, 1);
  doc.querySelector('#edit-search').click();
  assert.equal(doc.activeElement.id, 'city');
  dom.window.close();
});
