import test from 'node:test';
import assert from 'node:assert/strict';

function element(tagName) {
  return {
    tagName, children: [], className: '', textContent: '',
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = children; },
  };
}

test('loads a listing detail from a direct URL and renders its fields', async () => {
  const previous = { document: globalThis.document, window: globalThis.window, fetch: globalThis.fetch };
  const feedback = { textContent: '' };
  const detailFeedback = { textContent: '' };
  const detailSection = { hidden: true };
  const detailContent = element('div');
  const input = { value: '', min: '', addEventListener() {} };
  const searchForm = { addEventListener() {} };
  let requestedUrl;
  globalThis.document = {
    querySelector(selector) {
      return {
        '#search-form': searchForm, '#search-feedback': feedback,
        '#search-results': element('div'), '#checkin': input, '#checkout': input,
        '#listing-detail': detailSection, '#detail-feedback': detailFeedback, '#detail-content': detailContent,
      }[selector] || null;
    },
    createElement: element,
  };
  globalThis.window = { location: { search: '?listing=listing-123' } };
  globalThis.fetch = async (url) => {
    requestedUrl = url.toString();
    return { ok: true, async json() {
      return { id: 'listing-123', title: 'Casa segura', description: 'Cerca del parque', city: 'Bogotá',
        location: 'Chapinero', type: 'APARTMENT', capacity: 4, pricePerNight: 180000,
        amenityIds: ['WIFI'], rules: 'No fumar.', imageReferences: ['/casa.jpg'] };
    } };
  };
  try {
    await import('../src/main.js?direct-detail-route-test');
    await new Promise(setImmediate);
    assert.equal(requestedUrl, 'http://localhost:8080/api/listings/listing-123');
    assert.equal(detailSection.hidden, false);
    assert.equal(detailFeedback.textContent, '');
    assert.equal(detailContent.children[0].children[1].children.find((child) => child.id === 'detail-title').textContent, 'Casa segura');
  } finally {
    globalThis.document = previous.document;
    globalThis.window = previous.window;
    globalThis.fetch = previous.fetch;
  }
});
