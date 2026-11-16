import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { JSDOM } from 'jsdom';
import { formatListingPrice, renderListingCard, safeListingImage } from '../src/listing-card.js';

function fixture(t) {
  const dom = new JSDOM('<main></main>', { url: 'http://marketplace.test/' });
  t.after(() => dom.window.close());
  return { doc: dom.window.document, win: dom.window };
}

test('tarjeta usa fotografías admitidas y recupera imagen rota con una ilustración local identificada', (t) => {
  const { doc, win } = fixture(t);
  const card = renderListingCard(doc, { id: 'one', title: 'Casa de prueba', type: 'HOUSE', city: 'Bogotá', capacity: 3,
    pricePerNight: 120000, imageReferences: ['javascript:alert(1)', '/foto-real.jpg'], amenityIds: ['WIFI', 'POOL'] },
  { href: '/?listing=one&checkin=2026-12-10' });
  doc.querySelector('main').append(card);
  const image = card.querySelector('.listing-card-image');
  assert.equal(image.getAttribute('src'), '/foto-real.jpg');
  assert.equal(card.querySelector('.listing-photo-placeholder').hidden, true);
  assert.deepEqual([...card.querySelectorAll('.listing-card-amenities li')].map((node) => node.textContent), ['Wi-Fi', 'Piscina']);
  assert.match(card.querySelector('.listing-price').textContent, /120\.000.*COP \/ noche/);
  assert.equal(card.querySelector('.listing-card-capacity').textContent, 'Hasta 3 huéspedes');
  for (const link of card.querySelectorAll('a')) assert.equal(link.getAttribute('href'), '/?listing=one&checkin=2026-12-10');
  image.dispatchEvent(new win.Event('error'));
  assert.equal(image.hidden, true);
  assert.equal(card.querySelector('.listing-photo-placeholder').hidden, false);
  assert.match(card.querySelector('.listing-photo-placeholder').textContent, /Ilustración.*Sin fotos publicadas/);
});

test('los cuatro tipos tienen ilustraciones locales propias y no inventan servicios ni valoraciones', (t) => {
  const { doc } = fixture(t);
  const types = { HOUSE: 'house', APARTMENT: 'apartment', ROOM: 'room', CABIN: 'cabin' };
  for (const [type, name] of Object.entries(types)) {
    const card = renderListingCard(doc, { id: type, title: '<img src=x>', type });
    const illustration = card.querySelector('.listing-photo-illustration');
    assert.match(illustration.src, new RegExp(`listing-${name}\\.svg$`));
    assert.ok(existsSync(new URL(`../src/assets/listing-${name}.svg`, import.meta.url)));
    assert.equal(card.querySelector('h3').textContent, '<img src=x>');
    assert.equal(card.querySelector('h3 img'), null);
    assert.match(card.querySelector('.listing-card-services-note').textContent, /Consulta los servicios/);
    assert.equal(card.querySelector('.listing-price').textContent, 'Precio no disponible');
    assert.equal(card.querySelector('[class*=rating]'), null);
  }
});

test('rechaza referencias ejecutables o con credenciales y no redondea precios con centavos', () => {
  for (const value of ['javascript:alert(1)', 'data:image/png;base64,abc', 'https://user:password@example.test/photo.jpg', '', null]) {
    assert.equal(safeListingImage(value), null);
  }
  assert.equal(safeListingImage('/foto.jpg'), '/foto.jpg');
  assert.equal(safeListingImage('https://images.example.test/foto.jpg'), 'https://images.example.test/foto.jpg');
  assert.match(formatListingPrice(120.5), /120,50/);
  assert.equal(formatListingPrice(undefined), 'Precio no disponible');
  assert.equal(formatListingPrice(-20), 'Precio no disponible');
});
