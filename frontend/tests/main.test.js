import test from 'node:test';
import assert from 'node:assert/strict';

test('conecta validación, búsqueda y mensajes de error de la interfaz', async () => {
  const previous = {
    document: globalThis.document,
    FormData: globalThis.FormData,
    fetch: globalThis.fetch,
  };
  const handlers = {};
  const formValues = {
    city: '', checkin: '2026-12-03', checkout: '2026-12-05', guests: '2',
    type: 'APARTMENT', minPrice: '100000', maxPrice: '200000', amenityIds: ['WIFI', 'POOL'],
  };
  const checkin = { value: '', min: '', addEventListener(name, handler) { handlers[`checkin:${name}`] = handler; } };
  const checkout = { value: '', min: '', addEventListener(name, handler) { handlers[`checkout:${name}`] = handler; } };
  const feedback = { textContent: '' };
  const cards = [];
  const results = {
    replaceChildren() { cards.length = 0; },
    append(card) { cards.push(card); },
  };
  const form = { addEventListener(name, handler) { handlers[name] = handler; } };
  globalThis.document = {
    querySelector(selector) {
      return { '#search-form': form, '#search-feedback': feedback, '#search-results': results,
        '#checkin': checkin, '#checkout': checkout }[selector] || null;
    },
    createElement(tagName) {
      return { tagName, append(...children) { this.children = children; } };
    },
  };
  globalThis.FormData = class {
    entries() { return Object.entries(formValues).filter(([key]) => key !== 'amenityIds').concat(formValues.amenityIds.map(value => ['amenityIds', value])); }
    getAll(key) { return key === 'amenityIds' ? formValues.amenityIds : []; }
  };
  globalThis.fetch = async (url) => {
    if (formValues.city === 'Error') return { ok: false };
    assert.match(url, /type=APARTMENT/);
    assert.match(url, /minPrice=100000/);
    assert.deepEqual(new URL(url).searchParams.getAll('amenityIds'), ['WIFI', 'POOL']);
    if (formValues.city === 'Sin resultados') {
      return { ok: true, async json() { return { total: 0, items: [] }; } };
    }
    return { ok: true, async json() { return { total: 1, items: [{ id: 'a4887b19-653a-4c03-b2d8-7717ebcd3205', title: 'Casa segura', city: 'Bogotá', type: 'APARTMENT', capacity: 4, pricePerNight: 180000 }] }; } };
  };

  try {
    await import('../src/main.js?main-ui-test');
    checkin.value = '2026-12-05';
    checkout.value = '2026-12-04';
    handlers['checkin:change']();
    assert.equal(checkout.min, '2026-12-05');
    assert.equal(checkout.value, '');

    handlers.submit({ preventDefault() {} });
    assert.match(feedback.textContent, /ciudad/i);
    assert.equal(cards.length, 0);

    formValues.city = 'Bogotá';
    handlers.submit({ preventDefault() {} });
    await new Promise(setImmediate);
    assert.match(feedback.textContent, /1 alojamiento encontrado/i);
    assert.equal(cards[0].children[0].textContent, 'Casa segura');
    assert.equal(cards[0].children[3].href, '/?listing=a4887b19-653a-4c03-b2d8-7717ebcd3205');
    assert.equal(cards[0].children[3].textContent, 'Ver alojamiento');

    formValues.city = 'Sin resultados';
    handlers.submit({ preventDefault() {} });
    await new Promise(setImmediate);
    assert.match(feedback.textContent, /no encontramos alojamientos/i);
    assert.equal(cards.length, 0);

    formValues.city = 'Error';
    handlers.submit({ preventDefault() {} });
    await new Promise(setImmediate);
    assert.match(feedback.textContent, /no fue posible consultar/i);
  } finally {
    globalThis.document = previous.document;
    globalThis.FormData = previous.FormData;
    globalThis.fetch = previous.fetch;
  }
});

test('ignora búsquedas anteriores al cambiar criterios o al mostrar un error de validación', async () => {
  const previous = { document: globalThis.document, FormData: globalThis.FormData, fetch: globalThis.fetch };
  const handlers = {};
  const values = { city: 'Bogotá', checkin: '2026-12-03', checkout: '2026-12-05', guests: '2' };
  const feedback = { textContent: '' };
  const cards = [];
  const form = { addEventListener: (name, handler) => { handlers[name] = handler; } };
  const input = () => ({ min: '', value: '', addEventListener() {} });
  const lookup = { '#search-form': form, '#search-feedback': feedback,
    '#search-results': { replaceChildren() { cards.length = 0; }, append(card) { cards.push(card); } },
    '#checkin': input(), '#checkout': input() };
  globalThis.document = { querySelector: (selector) => lookup[selector] || null,
    createElement: () => ({ append(...children) { this.children = children; } }) };
  globalThis.FormData = class { entries() { return Object.entries(values); } getAll() { return []; } };
  const pending = [];
  globalThis.fetch = () => new Promise((resolve) => pending.push(resolve));
  const response = (title) => ({ ok: true, json: async () => ({ total: 1, items: [
    { id: 'a4887b19-653a-4c03-b2d8-7717ebcd3205', title, city: 'Bogotá', capacity: 4, type: 'HOUSE', pricePerNight: 100000 },
  ] }) });
  try {
    await import('../src/main.js?search-race-test');
    handlers.submit({ preventDefault() {} });
    values.city = 'Medellín';
    handlers.submit({ preventDefault() {} });
    pending[1](response('Resultado nuevo'));
    await new Promise(setImmediate);
    pending[0](response('Resultado anterior'));
    await new Promise(setImmediate);
    assert.equal(cards[0].children[0].textContent, 'Resultado nuevo');
    handlers.submit({ preventDefault() {} });
    values.city = '';
    handlers.submit({ preventDefault() {} });
    pending[2](response('Resultado caducado'));
    await new Promise(setImmediate);
    assert.equal(cards.length, 0);
    assert.match(feedback.textContent, /ciudad/i);
  } finally {
    globalThis.document = previous.document;
    globalThis.FormData = previous.FormData;
    globalThis.fetch = previous.fetch;
  }
});
