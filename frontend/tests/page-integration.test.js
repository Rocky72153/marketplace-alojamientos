import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const settle = () => new Promise(setImmediate);
let pageSequence = 0;

async function page(run, { role, path = '/' } = {}) {
  const dom = new JSDOM(html, { url: `http://marketplace.test${path}` });
  const keys = ['window', 'document', 'location', 'localStorage', 'FormData', 'fetch'];
  const saved = Object.fromEntries(keys.map((key) => [key, globalThis[key]]));
  for (const key of keys.filter((key) => key !== 'fetch')) globalThis[key] = dom.window[key];
  dom.window.scrollTo = () => {};
  const session = { accessToken: 'test-session', expiresAt: '2099-01-01T00:00:00Z',
    user: { id: 'user-1', name: 'Usuario de prueba', role: role || 'GUEST', email: 'test@example.invalid' } };
  if (role) dom.window.localStorage.setItem('marketplace-session', JSON.stringify(session));
  let rejectLogin = false;
  globalThis.fetch = async (input) => {
    const url = new URL(input);
    let body = [];
    let ok = true;
    if (url.pathname.endsWith('/cities')) body = ['Bogotá', 'Medellín'];
    else if (url.pathname.startsWith('/api/stripe-test/checkouts/')) body = {
      attemptId: url.pathname.split('/').at(-1), status: 'PAID',
    };
    else if (url.pathname.endsWith('/suggestions')) body = { items: [], total: 0 };
    else if (url.pathname === '/api/auth/login') {
      ok = !rejectLogin;
      body = ok ? session : { message: 'Correo o contraseña incorrectos.' };
    } else if (url.pathname.startsWith('/api/admin')) { ok = false; body = { message: 'Error de prueba visible.' }; }
    return { ok, status: ok ? 200 : 401, json: async () => body };
  };
  try {
    await import(`../src/main.js?integration=${++pageSequence}`);
    await settle();
    await run({ doc: dom.window.document, win: dom.window, rejectLogin: (value) => { rejectLogin = value; } });
    await settle();
  } finally {
    dom.window.close();
    for (const key of keys) globalThis[key] = saved[key];
  }
}

function assertView(doc, view) {
  assert.equal(doc.body.dataset.view, view);
  assert.deepEqual([...doc.querySelectorAll('.app-view')].filter((node) => !node.hidden).map((node) => node.id), [`view-${view}`]);
}

test('real page: guest navigation, auth errors, login, private sections and logout stay usable', async () => {
  await page(async ({ doc, win, rejectLogin }) => {
    assertView(doc, 'explore');
    for (const id of ['account-link', 'reservation-link', 'notifications-link', 'host-link']) {
      doc.getElementById(id).click();
      assertView(doc, 'account');
      assert.equal(doc.getElementById('account-panel').hidden, false);
    }
    doc.querySelector('[data-view-link="explore"]').click();
    assertView(doc, 'explore');
    doc.getElementById('host-entry').click();
    assertView(doc, 'account');
    doc.getElementById('register-tab').click();
    assert.equal(doc.getElementById('register-form').hidden, false);
    assert.equal(doc.getElementById('login-form').hidden, true);
    doc.getElementById('login-tab').click();
    const form = doc.getElementById('login-form');
    form.elements.email.value = 'test@example.invalid';
    form.elements.password.value = 'Test-password-123';
    rejectLogin(true);
    form.dispatchEvent(new win.Event('submit', { bubbles: true, cancelable: true }));
    await settle();
    assert.match(doc.getElementById('account-feedback').textContent, /incorrectos/);
    assertView(doc, 'account');
    rejectLogin(false);
    form.dispatchEvent(new win.Event('submit', { bubbles: true, cancelable: true }));
    await settle();
    assert.equal(doc.getElementById('session-panel').hidden, false);
    doc.getElementById('reservation-link').click();
    await settle();
    assertView(doc, 'reservations');
    assert.match(doc.getElementById('reservation-history-feedback').textContent, /Aún no tienes/);
    doc.getElementById('notifications-link').click();
    await settle();
    assertView(doc, 'notifications');
    assert.match(doc.getElementById('notifications-items').textContent, /Aún no tienes/);
    doc.getElementById('account-link').click();
    doc.getElementById('logout-button').click();
    assertView(doc, 'account');
    assert.equal(win.localStorage.getItem('marketplace-session'), null);
    doc.getElementById('reservation-link').click();
    assertView(doc, 'account');
  });
});

test('real page: host workspace opens directly and role links reflect the session', async () => {
  await page(async ({ doc }) => {
    assertView(doc, 'host');
    assert.equal(doc.getElementById('host-link').hidden, false);
    assert.equal(doc.getElementById('reservation-link').hidden, true);
    assert.equal(doc.getElementById('admin-workspace-link').hidden, true);
    doc.getElementById('account-link').click();
    assertView(doc, 'account');
    doc.getElementById('host-link').click();
    assertView(doc, 'host');
  }, { role: 'HOST', path: '/#anfitrion' });
});

test('real page: Stripe return resumes its status view after login and confirms only the server response', async () => {
  await page(async ({ doc, win }) => {
    assertView(doc, 'payment');
    assert.match(doc.getElementById('payment-return').textContent, /Inicia sesión/);
    doc.getElementById('payment-account-link').click();
    assertView(doc, 'account');
    assert.equal(doc.getElementById('account-payment-return').hidden, false);
    const form = doc.getElementById('login-form');
    form.elements.email.value = 'test@example.invalid';
    form.elements.password.value = 'Test-password-123';
    form.dispatchEvent(new win.Event('submit', { bubbles: true, cancelable: true }));
    await settle();
    assertView(doc, 'payment');
    assert.match(doc.getElementById('payment-return').textContent, /confirmado por el servidor/);
  }, { path: '/?stripe_checkout=17dbe8dc-b789-4a7c-a64b-b3a784d114a0' });
});

test('real page: admin links open exclusive views and service errors do not reset navigation', async () => {
  await page(async ({ doc }) => {
    assert.equal(doc.getElementById('admin-workspace-link').hidden, false);
    doc.getElementById('admin-workspace-link').click();
    await settle();
    assertView(doc, 'admin');
    assert.match(doc.getElementById('admin-workspace-content').textContent, /Error de prueba visible/);
    doc.getElementById('admin-reviews-link').click();
    await settle();
    assertView(doc, 'moderation');
    assert.equal(doc.getElementById('reservation-link').hidden, true);
  }, { role: 'ADMIN' });
});
