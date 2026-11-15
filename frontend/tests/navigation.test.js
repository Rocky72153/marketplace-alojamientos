import test from 'node:test';
import assert from 'node:assert/strict';
import { initNavigation, permittedView, resolveView } from '../src/navigation.js';

function node(attributes = {}) {
  return {
    hidden: false, attributes: { ...attributes }, handlers: {}, textContent: '',
    setAttribute(name, value) { this.attributes[name] = value; },
    getAttribute(name) { return this.attributes[name]; },
    removeAttribute(name) { delete this.attributes[name]; },
    addEventListener(name, callback) { this.handlers[name] = callback; },
    focus() {},
  };
}
function fixture(start = 'http://localhost/') {
  const views = ['explore', 'account', 'reservations', 'notifications', 'host', 'admin', 'moderation', 'detail', 'payment'];
  const nodes = Object.fromEntries(views.map((view) => [`#view-${view}`, node()]));
  for (const id of ['account-panel', 'account-title', 'account-feedback', 'login-form', 'register-form', 'auth-tabs', 'notifications-panel', 'reservation-history']) nodes[`#${id}`] = node();
  const links = views.map((view) => node({ 'data-view-link': view }));
  const tabs = ['login', 'register'].map((tab) => node({ 'data-auth-tab': tab }));
  const doc = { body: node(), querySelector: (selector) => nodes[selector] || null,
    querySelectorAll: (selector) => selector === '[data-view-link]' ? links : selector === '[data-auth-tab]' ? tabs : [] };
  const events = {};
  const win = { location: new URL(start), addEventListener: (name, callback) => { events[name] = callback; },
    history: { pushState(_data, _unused, url) { win.location = new URL(url); } }, scrollTo() {} };
  const visible = () => views.filter((view) => !nodes[`#view-${view}`].hidden);
  return { doc, win, nodes, links, tabs, events, visible };
}

test('direct listing and Stripe returns open their own view, and a navigation hash takes precedence', () => {
  assert.equal(resolveView({ search: '?listing=abc' }), 'detail');
  assert.equal(resolveView({ search: '?stripe_checkout=abc' }), 'payment');
  assert.equal(resolveView({ search: '?listing=abc', hash: '#mi-cuenta' }), 'account');
  assert.equal(resolveView({ hash: '#alojamiento' }), 'explore');
  assert.equal(resolveView({ hash: '#desconocida' }), 'explore');
  assert.equal(resolveView({ hash: '#__proto__' }), 'explore');
  assert.equal(resolveView({ hash: '#toString' }), 'explore');
});

test('asynchronous contents cannot reveal other task views and navigation preserves return context', () => {
  const f = fixture('http://localhost/?listing=listing-1');
  const session = { user: { role: 'GUEST' } };
  const router = initNavigation(f.doc, f.win, () => session);
  assert.deepEqual(f.visible(), ['detail']);
  f.nodes['#notifications-panel'].hidden = false;
  f.nodes['#reservation-history'].hidden = false;
  assert.deepEqual(f.visible(), ['detail']);
  router.navigate('account');
  assert.deepEqual(f.visible(), ['account']);
  assert.equal(f.win.location.search, '?listing=listing-1');
  router.navigate('explore');
  assert.deepEqual(f.visible(), ['explore']);
  assert.equal(f.win.location.search, '');
  assert.equal(f.links[0].attributes['aria-current'], 'page');
  router.navigate('__proto__');
  assert.deepEqual(f.visible(), ['explore']);
});

test('protected routes resume after login and leave the private view after logout', () => {
  const f = fixture('http://localhost/#reservas');
  let session = null;
  const router = initNavigation(f.doc, f.win, () => session);
  assert.deepEqual(f.visible(), ['account']);
  session = { user: { role: 'GUEST' } };
  router.syncSession();
  assert.deepEqual(f.visible(), ['reservations']);
  session = null;
  router.syncSession();
  assert.deepEqual(f.visible(), ['account']);
  for (const view of ['host', 'admin', 'moderation']) assert.equal(permittedView(view, { user: { role: 'GUEST' } }), 'account');
  assert.equal(permittedView('host', { user: { role: 'HOST' } }), 'host');
  assert.equal(permittedView('admin', { user: { role: 'ADMIN' } }), 'admin');
});

test('back navigation restores the view and account forms are exclusive keyboard-accessible tabs', () => {
  const f = fixture();
  const router = initNavigation(f.doc, f.win, () => null);
  router.navigate('account');
  assert.equal(f.nodes['#login-form'].hidden, false);
  assert.equal(f.nodes['#register-form'].hidden, true);
  f.tabs[1].handlers.click();
  assert.equal(f.nodes['#login-form'].hidden, true);
  assert.equal(f.nodes['#register-form'].hidden, false);
  assert.equal(f.tabs[1].attributes['aria-selected'], 'true');
  f.tabs[1].handlers.keydown({ key: 'ArrowLeft', preventDefault() {} });
  assert.equal(f.nodes['#login-form'].hidden, false);
  f.win.location = new URL('http://localhost/#explorar');
  f.events.popstate();
  assert.deepEqual(f.visible(), ['explore']);
});
