import test from 'node:test';
import assert from 'node:assert/strict';
import { initAccountTools } from '../src/account-tools.js';

class Element {
  constructor(tagName) {
    this.tagName = tagName; this.children = []; this.listeners = {};
    this.textContent = ''; this.value = ''; this.hidden = false; this.disabled = false;
  }
  append(...children) { this.children.push(...children); }
  setAttribute() {}
  addEventListener(name, fn) { this.listeners[name] = fn; }
  async dispatch(name) { return this.listeners[name]?.({ preventDefault() {} }); }
}
const doc = { createElement: (tag) => new Element(tag) };
const ok = (body) => ({ ok: true, json: async () => body });
const failed = (message) => ({ ok: false, json: async () => ({ message }) });
const tick = () => new Promise((resolve) => setImmediate(resolve));
function find(node, predicate) {
  if (predicate(node)) return node;
  for (const child of node.children || []) {
    const result = find(child, predicate);
    if (result) return result;
  }
  return null;
}
const byText = (node, text) => find(node, (x) => x.textContent === text);
const byName = (node, name) => find(node, (x) => x.name === name);

test('carga y actualiza únicamente el perfil de la sesión', async () => {
  const calls = []; const updated = [];
  const fetchImpl = async (url, options) => {
    calls.push({ path: url.pathname, options });
    if (options.method === 'PATCH') return ok({ name: 'Ana Actualizada', email: 'ana@example.test', role: 'GUEST' });
    return ok({ name: 'Ana', email: 'ana@example.test', role: 'GUEST', phone: '300', photoReference: '' });
  };
  const root = new Element('div');
  const panel = initAccountTools(root, 'http://localhost:8080', { onProfileUpdated: (user) => updated.push(user) }, doc, fetchImpl);
  panel.sync({ accessToken: 'own-token', user: { id: 'u1' } });
  await tick();
  assert.ok(byText(root, 'ana@example.test · GUEST'));
  assert.equal(byName(root, 'name').value, 'Ana');
  byName(root, 'name').value = '  Ana Actualizada  ';
  byName(root, 'phone').value = '  311  ';
  const form = find(root, (node) => node.tagName === 'form' && find(node, (field) => field.name === 'name'));
  await form.dispatch('submit');
  const change = calls.find((call) => call.options.method === 'PATCH');
  assert.equal(change.path, '/api/profile/me');
  assert.equal(change.options.headers.Authorization, 'Bearer own-token');
  assert.deepEqual(JSON.parse(change.options.body), { name: 'Ana Actualizada', phone: '311', photoReference: '' });
  assert.equal(updated[0].name, 'Ana Actualizada');
  assert.ok(byText(root, 'Perfil actualizado.'));
});

test('recuperación es neutral y aclara que no envía correo ni cambia contraseña', async () => {
  const calls = [];
  const root = new Element('div');
  initAccountTools(root, 'http://localhost:8080', {}, doc, async (url, options) => {
    calls.push({ path: url.pathname, options });
    return ok({ message: 'No se muestra una respuesta que prometa correo.' });
  });
  const email = byName(root, 'email'); email.value = ' persona@example.test ';
  const form = find(root, (node) => node.tagName === 'form' && find(node, (field) => field.name === 'email'));
  await form.dispatch('submit');
  assert.equal(calls[0].path, '/api/auth/recovery');
  assert.deepEqual(JSON.parse(calls[0].options.body), { email: 'persona@example.test' });
  assert.equal(calls[0].options.headers.Authorization, undefined);
  assert.ok(byText(root, 'Solicitud simulada registrada. No se ha enviado un correo ni se ha cambiado la contraseña.'));
});

test('desactivación exige confirmación y solo cierra sesión al recibir éxito', async () => {
  const calls = []; let logoutCount = 0; let allowDelete = false;
  const root = new Element('div');
  const panel = initAccountTools(root, 'http://localhost:8080', { onDeactivated: () => { logoutCount += 1; } },
    doc, async (url, options) => {
      calls.push({ path: url.pathname, options });
      if (options.method === 'DELETE') return allowDelete ? ok({ message: 'Desactivada' }) : failed('No se pudo desactivar.');
      return ok({ name: 'Ana', email: 'ana@example.test', role: 'GUEST' });
    });
  panel.sync({ accessToken: 'own-token', user: { id: 'u1' } });
  await tick();
  const confirm = byText(root, 'Confirmar desactivación');
  assert.equal(find(root, (node) => node.children.includes(confirm)).hidden, true);
  assert.equal(calls.filter((call) => call.options.method === 'DELETE').length, 0);
  await byText(root, 'Desactivar mi cuenta').dispatch('click');
  assert.equal(find(root, (node) => node.children.includes(confirm)).hidden, false);
  await confirm.dispatch('click');
  assert.equal(logoutCount, 0);
  assert.ok(byText(root, 'No se pudo desactivar.'));
  allowDelete = true;
  await confirm.dispatch('click');
  assert.equal(logoutCount, 1);
  assert.equal(calls.filter((call) => call.options.method === 'DELETE').length, 2);
});

test('descarta respuestas tardías y borra datos al cambiar o cerrar sesión', async () => {
  const pending = [];
  const root = new Element('div');
  const panel = initAccountTools(root, 'http://localhost:8080', {}, doc,
    () => new Promise((resolve) => pending.push(resolve)));
  panel.sync({ accessToken: 'first-token', user: { id: 'u1' } });
  panel.sync({ accessToken: 'second-token', user: { id: 'u2' } });
  pending[1](ok({ name: 'Segundo', email: 'segundo@example.test', role: 'HOST' }));
  await tick();
  pending[0](ok({ name: 'Primero', email: 'primero@example.test', role: 'GUEST' }));
  await tick();
  assert.equal(byName(root, 'name').value, 'Segundo');
  assert.ok(byText(root, 'segundo@example.test · HOST'));
  panel.clear();
  assert.equal(byName(root, 'name').value, '');
  assert.equal(byText(root, 'Mi perfil') && find(root, (node) => node.children.includes(byText(root, 'Mi perfil'))).hidden, true);
});

test('una actualización tardía no modifica la nueva sesión', async () => {
  let resolvePatch; const updated = [];
  const root = new Element('div');
  const panel = initAccountTools(root, 'http://localhost:8080', { onProfileUpdated: (user) => updated.push(user) },
    doc, async (_url, options) => {
      if (options.method === 'PATCH') return new Promise((resolve) => { resolvePatch = resolve; });
      if (options.headers.Authorization === 'Bearer first-token') return ok({ name: 'Primero', email: 'primero@example.test', role: 'GUEST' });
      return ok({ name: 'Segundo', email: 'segundo@example.test', role: 'HOST' });
    });
  panel.sync({ accessToken: 'first-token', user: { id: 'u1' } });
  await tick();
  const form = find(root, (node) => node.tagName === 'form' && find(node, (field) => field.name === 'name'));
  const saving = form.dispatch('submit');
  panel.sync({ accessToken: 'second-token', user: { id: 'u2' } });
  await tick();
  resolvePatch(ok({ name: 'Respuesta vieja', email: 'primero@example.test', role: 'GUEST' }));
  await saving;
  assert.equal(updated.length, 0);
  assert.equal(byName(root, 'name').value, 'Segundo');
  assert.ok(byText(root, 'segundo@example.test · HOST'));
});
