import test from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { buildListingDetailUrl, fetchListingDetail, renderListingDetail, renderListingDetailError } from '../src/detail.js';

function fixture(t) {
  const dom = new JSDOM('<main id="detail"></main>', { url: 'http://marketplace.test/' });
  t.after(() => dom.window.close());
  return { doc: dom.window.document, win: dom.window, container: dom.window.document.querySelector('#detail') };
}
const listing = { title: 'Casa segura', description: 'Alojamiento con patio.', city: 'Bogotá', location: 'Chapinero',
  type: 'APARTMENT', capacity: 4, pricePerNight: 180000, hostName: 'Anfitrión', amenityIds: ['WIFI', 'POOL'],
  rules: 'No fumar', cancellationPolicy: { freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 },
  imageReferences: ['/casa.jpg', 'https://images.example.test/casa-2.jpg', 'javascript:alert(1)', 'data:image/svg+xml,unsafe'] };

test('builds a shareable detail URL for a result card', () => {
  assert.equal(buildListingDetailUrl('listing 1'), '/?listing=listing%201');
  assert.throws(() => buildListingDetailUrl(''), /identificador/i);
});

test('fetches listing detail and gives a public unavailable message for hidden listings', async () => {
  let requestedUrl;
  const result = await fetchListingDetail('listing-id', 'http://localhost:8080', async (url) => {
    requestedUrl = url.toString();
    return { ok: true, async json() { return { id: 'listing-id', title: 'Casa de prueba' }; } };
  });
  assert.equal(requestedUrl, 'http://localhost:8080/api/listings/listing-id');
  assert.equal(result.title, 'Casa de prueba');
  await assert.rejects(fetchListingDetail('hidden-id', 'http://localhost:8080', async () => ({ ok: false, status: 404 })), /no está disponible/i);
});

test('renders real details, services, rules, cancellation policy and safe photographs', (t) => {
  const f = fixture(t);
  renderListingDetail(f.container, listing, f.doc);
  assert.equal(f.doc.querySelector('#detail-title').textContent, 'Casa segura');
  assert.match(f.doc.querySelector('.detail-location').textContent, /Chapinero · Bogotá/);
  assert.match(f.doc.querySelector('.detail-facts').textContent, /Apartamento.*Hasta 4 huéspedes.*Anfitrión/);
  assert.match(f.doc.querySelector('.detail-description').textContent, /con patio/);
  assert.deepEqual([...f.doc.querySelectorAll('.detail-amenity')].map((node) => node.textContent), ['Wi-Fi', 'Piscina']);
  assert.equal(f.doc.querySelector('.detail-rules').textContent, 'No fumar');
  assert.match(f.doc.querySelector('.detail-cancellation').textContent, /100 % con 7 días.*50 %.*48 horas.*Sin reembolso/);
  assert.equal(f.doc.querySelector('.detail-main-image').getAttribute('src'), '/casa.jpg');
  assert.equal(f.doc.querySelectorAll('.detail-thumbnail').length, 2);
  assert.equal([...f.doc.querySelectorAll('img')].some((image) => /^(javascript|data):/.test(image.getAttribute('src'))), false);
  assert.equal(f.doc.querySelectorAll('[class*=rating]').length, 0);
});

test('gallery supports thumbnails, keyboard selection and a recoverable broken photograph', (t) => {
  const f = fixture(t);
  renderListingDetail(f.container, listing, f.doc);
  const image = f.doc.querySelector('.detail-main-image');
  const buttons = [...f.doc.querySelectorAll('.detail-thumbnail')];
  assert.equal(buttons[0].getAttribute('aria-pressed'), 'true');
  buttons[1].click();
  assert.match(image.src, /casa-2.jpg$/);
  assert.match(image.alt, /fotografía 2 de 2/);
  assert.equal(buttons[0].getAttribute('aria-pressed'), 'false');
  assert.equal(buttons[1].getAttribute('aria-pressed'), 'true');
  image.dispatchEvent(new f.win.Event('error'));
  assert.equal(image.hidden, true);
  assert.equal(f.doc.querySelector('.detail-gallery-stage .detail-no-image').hidden, false);
  buttons[1].dispatchEvent(new f.win.KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true, cancelable: true }));
  assert.equal(f.doc.activeElement, buttons[0]);
  assert.match(image.src, /casa.jpg$/);
  assert.equal(image.hidden, false);
  assert.equal(f.doc.querySelector('.detail-gallery-stage .detail-no-image').hidden, true);
  assert.equal(f.doc.querySelector('.detail-gallery-caption').textContent, 'Fotografía 1 de 2');
});

test('missing photos and policy stay honest and display a local illustration for the listing type', (t) => {
  const f = fixture(t);
  renderListingDetail(f.container, { type: 'CABIN', title: '<img src=x onerror=alert(1)>' }, f.doc);
  assert.match(f.doc.querySelector('.detail-no-image').textContent, /Ilustración.*Sin fotos publicadas/);
  assert.match(f.doc.querySelector('.listing-photo-illustration').src, /assets\/listing-cabin.svg$/);
  assert.equal(f.doc.querySelector('#detail-title').textContent, '<img src=x onerror=alert(1)>');
  assert.equal(f.doc.querySelector('#detail-title img'), null);
  assert.equal(f.doc.querySelector('.detail-price').textContent, 'Precio no disponible');
  assert.match(f.doc.querySelector('.detail-cancellation').textContent, /Consulta la política vigente/);
  assert.equal(f.doc.querySelector('.detail-cancellation-rules'), null);
});

test('renders a friendly public detail error', (t) => {
  const f = fixture(t);
  renderListingDetailError(f.container, 'Este alojamiento no está disponible.', f.doc);
  assert.equal(f.container.firstElementChild.textContent, 'Este alojamiento no está disponible.');
  assert.equal(f.container.firstElementChild.getAttribute('role'), 'status');
});
