import test from 'node:test';
import assert from 'node:assert/strict';
import { initReviewForm, initReviewList, initReviewModeration } from '../src/reviews.js';

class Element {
  constructor(tagName) {
    this.tagName = tagName; this.children = []; this.listeners = {}; this.textContent = '';
    this.hidden = false; this.disabled = false; this.value = '';
  }
  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  setAttribute() {}
  addEventListener(name, callback) { this.listeners[name] = callback; }
  async dispatch(name) { return this.listeners[name]?.({ preventDefault() {} }); }
}

const documentRef = { createElement: (tag) => new Element(tag), querySelector: () => null };
function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) {
    const found = find(child, predicate);
    if (found) return found;
  }
  return null;
}
const ok = (body) => ({ ok: true, json: async () => body });
const wait = () => new Promise((resolve) => setImmediate(resolve));

test('lista pública dibuja el contenido como texto y calcula solo las reseñas recibidas', async () => {
  const container = new Element('div');
  const text = '<script>no ejecutar</script>';
  const panel = initReviewList(container, { id: 'listing-1' }, 'http://localhost:8080', documentRef,
    async () => ok([{ guestDisplayName: 'Ana', rating: 4, content: text, createdAt: '2026-01-01T12:00:00Z' }]));
  await panel.refresh();
  assert.ok(find(panel.section, (node) => node.textContent === text));
  assert.match(find(panel.section, (node) => /promedio/.test(node.textContent)).textContent, /4\.0 de 5/);
});

test('una respuesta antigua no repone una reseña ocultada después de refrescar', async () => {
  const container = new Element('div');
  let resolveOld;
  const oldResponse = new Promise((resolve) => { resolveOld = resolve; });
  let calls = 0;
  const panel = initReviewList(container, { id: 'listing-1' }, 'http://localhost:8080', documentRef, async () => {
    calls += 1;
    return calls === 1 ? oldResponse : ok([]);
  });
  await panel.refresh();
  resolveOld(ok([{ guestDisplayName: 'Ana', rating: 5, content: 'Oculta' }]));
  await wait();
  assert.equal(find(panel.section, (node) => node.textContent === 'Oculta'), null);
  assert.match(find(panel.section, (node) => /no hay reseñas/i.test(node.textContent)).textContent, /no hay reseñas/i);
});

test('informa si falla la carga inicial de reseñas públicas', async () => {
  const panel = initReviewList(new Element('div'), { id: 'listing-1' }, 'http://localhost:8080', documentRef,
    async () => { throw new Error('No hay conexión'); });
  await wait();
  assert.ok(find(panel.section, (node) => node.textContent === 'No hay conexión'));
});

test('solo una reserva completada ofrece formulario y envía reseña vinculada', async () => {
  const container = new Element('div');
  assert.equal(initReviewForm(container, { id: 'b', listingId: 'l', status: 'CONFIRMED' }, 'http://localhost:8080', () => 'token', () => {}, documentRef), null);
  let sent; let changed = false;
  const panel = initReviewForm(container, { id: 'booking-1', listingId: 'listing-1', status: 'COMPLETED' },
    'http://localhost:8080', () => 'guest-token', () => { changed = true; }, documentRef,
    async (url, options) => { sent = { url: url.toString(), options }; return ok({ id: 'review-1' }); });
  const form = find(panel.section, (node) => node.tagName === 'form');
  find(form, (node) => node.tagName === 'textarea').value = '  Un buen lugar  ';
  await form.dispatch('submit');
  assert.equal(sent.url, 'http://localhost:8080/api/listings/listing-1/reviews');
  assert.equal(sent.options.headers.Authorization, 'Bearer guest-token');
  assert.deepEqual(JSON.parse(sent.options.body), { bookingId: 'booking-1', rating: 5, content: 'Un buen lugar' });
  assert.equal(form.hidden, true);
  assert.equal(changed, true);
});

test('la moderación requiere motivo y vuelve a consultar tras ocultar', async () => {
  let hidden = false; let changed = false; const requests = [];
  const review = { id: 'r1', listingTitle: 'Casa', guestDisplayName: 'Ana', rating: 5, content: 'Bien', hidden: false };
  const panel = initReviewModeration(new Element('div'), 'http://localhost:8080', () => 'admin-token',
    () => { changed = true; }, documentRef, async (url, options = {}) => {
      requests.push({ path: url.pathname, options });
      if (url.pathname.endsWith('/hide')) { hidden = true; return ok({ ...review, hidden: true, hiddenReason: 'Incumple reglas' }); }
      return ok([{ ...review, hidden, hiddenReason: hidden ? 'Incumple reglas' : null }]);
    });
  await wait();
  const form = find(panel.section, (node) => node.tagName === 'form');
  await form.dispatch('submit');
  assert.equal(requests.filter((r) => r.path.endsWith('/hide')).length, 0);
  find(form, (node) => node.tagName === 'textarea').value = ' Incumple reglas ';
  await form.dispatch('submit');
  assert.equal(requests.filter((r) => r.path.endsWith('/hide')).length, 1);
  assert.deepEqual(JSON.parse(requests.find((r) => r.path.endsWith('/hide')).options.body), { reason: 'Incumple reglas' });
  assert.equal(changed, true);
  assert.match(find(panel.section, (node) => /Oculta\. Motivo/.test(node.textContent)).textContent, /Incumple reglas/);
});

test('el cierre de sesión elimina datos de moderación y descarta respuestas atrasadas', async () => {
  let token = 'admin-token'; let resolveResponse;
  const deferred = new Promise((resolve) => { resolveResponse = resolve; });
  const panel = initReviewModeration(new Element('div'), 'http://localhost:8080', () => token,
    () => {}, documentRef, () => deferred);
  panel.clear(); token = '';
  resolveResponse(ok([{ listingTitle: 'Privado', guestDisplayName: 'Ana', rating: 5, content: 'No mostrar' }]));
  await wait();
  assert.equal(panel.section.hidden, true);
  assert.equal(find(panel.section, (node) => node.textContent === 'No mostrar'), null);
});

test('informa un fallo inesperado al iniciar la moderación', async () => {
  const panel = initReviewModeration(new Element('div'), 'http://localhost:8080',
    () => { throw new Error('Sesión no disponible'); }, () => {}, documentRef);
  await wait();
  assert.ok(find(panel.section, (node) => node.textContent === 'No fue posible consultar las reseñas.'));
});
