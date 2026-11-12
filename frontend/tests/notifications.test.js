import test from 'node:test';
import assert from 'node:assert/strict';
import { initNotifications } from '../src/notifications.js';

class Element {
  constructor(tag) {
    this.tagName = tag;
    this.children = [];
    this.listeners = {};
    this.hidden = false;
    this.disabled = false;
    this.textContent = '';
  }

  append(...nodes) { this.children.push(...nodes); }
  replaceChildren(...nodes) { this.children = nodes; }
  addEventListener(name, handler) { this.listeners[name] = handler; }
  scrollIntoView() { this.scrolled = true; }
  dispatch(name, event = {}) {
    event.preventDefault ||= () => {};
    return this.listeners[name]?.(event);
  }
}

function fixture(fetchImpl, initialToken = 'token-a') {
  const container = new Element('section');
  const items = new Element('div');
  const feedback = new Element('p');
  const link = new Element('a');
  const accountPanel = new Element('section');
  const accountFeedback = new Element('p');
  const documentRef = {
    createElement: (tag) => new Element(tag),
    querySelector: (selector) => ({
      '#notifications-items': items,
      '#notifications-feedback': feedback,
      '#notifications-link': link,
      '#account-panel': accountPanel,
      '#account-feedback': accountFeedback,
    })[selector] || null,
  };
  let token = initialToken;
  const api = initNotifications(container, 'http://localhost:8080', () => token, fetchImpl, documentRef);
  return { api, container, items, feedback, link, accountPanel, accountFeedback, setToken: (value) => { token = value; } };
}

test('muestra solamente la respuesta autenticada y permite marcarla como leída', async () => {
  const calls = [];
  let read = false;
  const app = fixture(async (url, options = {}) => {
    calls.push({ path: url.pathname, options });
    if (options.method === 'PATCH') {
      read = true;
      return { ok: true, async json() { return {}; } };
    }
    return { ok: true, async json() {
      return [{ id: 'notice-1', message: '<b>Reserva confirmada</b>', createdAt: '2026-11-11T12:00:00Z',
        readAt: read ? '2026-11-11T13:00:00Z' : null }];
    } };
  });
  await app.api.refresh();

  assert.equal(app.container.hidden, false);
  assert.equal(calls[0].path, '/api/notifications');
  assert.equal(calls[0].options.headers.Authorization, 'Bearer token-a');
  assert.equal(app.items.children[0].children[0].textContent, '<b>Reserva confirmada</b>');
  assert.match(app.items.children[0].children[1].textContent, /Sin leer/);
  const button = app.items.children[0].children[2];
  await button.dispatch('click');
  assert.equal(calls[1].path, '/api/notifications/notice-1/read');
  assert.equal(calls[1].options.method, 'PATCH');
  assert.equal(calls[1].options.headers.Authorization, 'Bearer token-a');
  assert.match(app.items.children[0].children[1].textContent, /Leída/);
  assert.equal(app.items.children[0].children.length, 2);
});

test('pide iniciar sesión y no conserva contenido al cerrar sesión', async () => {
  const app = fixture(async () => { throw new Error('No debe consultar sin sesión'); }, null);
  app.container.hidden = false;
  app.items.append(new Element('p'));
  await app.api.refresh();
  assert.equal(app.container.hidden, true);
  assert.equal(app.items.children.length, 0);
  app.link.dispatch('click');
  assert.equal(app.accountPanel.hidden, false);
  assert.match(app.accountFeedback.textContent, /inicia sesión/i);
});

test('descarta la respuesta anterior cuando cambia la cuenta', async () => {
  const pending = [];
  const app = fixture(() => new Promise((resolve) => pending.push(resolve)));
  const first = app.api.refresh();
  app.setToken('token-b');
  const second = app.api.refresh();
  pending[1]({ ok: true, async json() { return [{ id: 'b', message: 'Mensaje de B', createdAt: '2026-11-11T12:00:00Z' }]; } });
  await second;
  pending[0]({ ok: true, async json() { return [{ id: 'a', message: 'Mensaje de A', createdAt: '2026-11-11T12:00:00Z' }]; } });
  await first;
  assert.equal(app.items.children.length, 1);
  assert.equal(app.items.children[0].children[0].textContent, 'Mensaje de B');
});

test('no vuelve a mostrar contenido si se cierra sesión durante la consulta', async () => {
  let resolveRequest;
  const app = fixture(() => new Promise((resolve) => { resolveRequest = resolve; }));
  const pending = app.api.refresh();
  app.setToken(null);
  app.api.clear();
  resolveRequest({ ok: true, async json() {
    return [{ id: 'a', message: 'Mensaje privado', createdAt: '2026-11-11T12:00:00Z' }];
  } });
  await pending;
  assert.equal(app.container.hidden, true);
  assert.equal(app.items.children.length, 0);
  assert.equal(app.feedback.textContent, '');
});

test('muestra el error sin marcar la notificación como leída', async () => {
  const app = fixture(async (_url, options = {}) => options.method === 'PATCH'
    ? { ok: false, async json() { return { message: 'Intenta de nuevo.' }; } }
    : { ok: true, async json() { return [{ id: '1', message: 'Pendiente', createdAt: 'inválida' }]; } });
  await app.api.refresh();
  const button = app.items.children[0].children[2];
  await button.dispatch('click');
  assert.equal(button.disabled, false);
  assert.match(app.feedback.textContent, /Intenta de nuevo/);
  assert.match(app.items.children[0].children[1].textContent, /Fecha no disponible/);
});
