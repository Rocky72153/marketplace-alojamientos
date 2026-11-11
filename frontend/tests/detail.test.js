import test from 'node:test';
import assert from 'node:assert/strict';
import { buildListingDetailUrl, fetchListingDetail, renderListingDetail, renderListingDetailError } from '../src/detail.js';

function element(tagName) {
  return {
    tagName, children: [], className: '', textContent: '',
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = children; },
  };
}

const documentRef = { createElement: element };

function textContentTree(node) {
  return [node.textContent, ...node.children.map(textContentTree)].join(' ');
}

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

test('renders comparison details, amenity labels, rules and safe image references', () => {
  const container = element('div');
  renderListingDetail(container, {
    title: 'Casa segura', description: 'Alojamiento con patio.', city: 'Bogotá', location: 'Chapinero',
    type: 'APARTMENT', capacity: 4, pricePerNight: 180000, hostName: 'Anfitrión',
    amenityIds: ['WIFI'], rules: 'No fumar', imageReferences: ['/casa.jpg', 'javascript:alert(1)'],
  }, documentRef);
  const article = container.children[0];
  const gallery = article.children[0];
  const content = article.children[1];
  assert.equal(gallery.children.length, 1);
  assert.equal(gallery.children[0].src, '/casa.jpg');
  const text = textContentTree(content);
  assert.match(text, /Casa segura/);
  assert.match(text, /Chapinero · Bogotá/);
  assert.match(text, /Apartamento/);
  assert.match(text, /Wi-Fi/);
  assert.match(text, /No fumar/);
});

test('renders a friendly public detail error', () => {
  const container = element('div');
  renderListingDetailError(container, 'Este alojamiento no está disponible.', documentRef);
  assert.equal(container.children[0].textContent, 'Este alojamiento no está disponible.');
});
