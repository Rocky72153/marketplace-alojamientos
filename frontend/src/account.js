import { initReviewForm } from './reviews.js';

const SESSION_KEY = 'marketplace-session';
const historyRequests = new WeakMap();

function readSession() {
  try {
    const session = JSON.parse(globalThis.localStorage?.getItem(SESSION_KEY) || 'null');
    const expiration = Date.parse(session?.expiresAt);
    if (!session?.accessToken || !session?.user || !Number.isFinite(expiration) || expiration <= Date.now()) {
      globalThis.localStorage?.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

async function sendAuthRequest(path, data, apiBase, fetchImpl) {
  const response = await fetchImpl(new URL(`/api/auth/${path}`, apiBase), {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(result.message || 'No fue posible completar la operación.');
  return result;
}

function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

export function initAccount(documentRef = globalThis.document, apiBase = 'http://localhost:8080', onSessionChange = () => {}, fetchImpl = globalThis.fetch) {
  let session = readSession();
  let authenticationRevision = 0;
  let registrationPending = false;
  const panel = documentRef?.querySelector('#account-panel');
  const accountLink = documentRef?.querySelector('#account-link');
  const reservationLink = documentRef?.querySelector('#reservation-link');
  const forms = documentRef?.querySelector('#account-forms');
  const loginForm = documentRef?.querySelector('#login-form');
  const registerForm = documentRef?.querySelector('#register-form');
  const sessionPanel = documentRef?.querySelector('#session-panel');
  const sessionSummary = documentRef?.querySelector('#session-summary');
  const feedback = documentRef?.querySelector('#account-feedback');
  const logoutButton = documentRef?.querySelector('#logout-button');

  function draw(message = '') {
    if (forms) forms.hidden = Boolean(session);
    if (sessionPanel) sessionPanel.hidden = !session;
    if (sessionSummary) sessionSummary.textContent = session
      ? `Sesión iniciada como ${session.user.name} (${session.user.role}).` : '';
    if (accountLink) accountLink.textContent = session ? 'Mi cuenta' : 'Iniciar sesión';
    if (feedback) feedback.textContent = message;
    onSessionChange(session);
  }

  async function authenticate(form, path) {
    if (!form || typeof FormData === 'undefined') return;
    if (path === 'register' && registrationPending) return;
    const registerButton = path === 'register' ? form.querySelector?.('button[type="submit"]') : null;
    if (path === 'register') {
      registrationPending = true;
      if (registerButton) registerButton.disabled = true;
    }
    const revision = ++authenticationRevision;
    if (feedback) feedback.textContent = 'Procesando…';
    try {
      const nextSession = await sendAuthRequest(path, formValues(form), apiBase, fetchImpl);
      if (revision !== authenticationRevision) return;
      session = nextSession;
      try { globalThis.localStorage?.setItem(SESSION_KEY, JSON.stringify(session)); } catch { /* Storage may be unavailable in private mode. */ }
      form.reset();
      draw(path === 'register' ? 'Cuenta creada e ingreso completado.' : 'Sesión iniciada.');
    } catch (error) {
      if (revision === authenticationRevision && feedback) feedback.textContent = error.message || 'No fue posible iniciar sesión.';
    } finally {
      if (path === 'register') {
        registrationPending = false;
        if (registerButton) registerButton.disabled = false;
      }
    }
  }

  accountLink?.addEventListener?.('click', (event) => {
    event.preventDefault();
    if (panel) panel.hidden = !panel.hidden;
    panel?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  });
  reservationLink?.addEventListener?.('click', (event) => {
    if (session) return;
    event.preventDefault();
    if (panel) panel.hidden = false;
    if (feedback) feedback.textContent = 'Inicia sesión para consultar tus reservas.';
    panel?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  });
  loginForm?.addEventListener?.('submit', (event) => {
    event.preventDefault();
    void authenticate(loginForm, 'login');
  });
  registerForm?.addEventListener?.('submit', (event) => {
    event.preventDefault();
    void authenticate(registerForm, 'register');
  });
  function logout(message = 'Sesión cerrada.') {
    authenticationRevision += 1;
    session = null;
    try { globalThis.localStorage?.removeItem(SESSION_KEY); } catch { /* Storage may be unavailable in private mode. */ }
    draw(message);
  }
  logoutButton?.addEventListener?.('click', () => logout());

  draw();
  return {
    getToken: () => session?.accessToken || null,
    getSession: () => session,
    logout,
    updateUser: (user) => {
      if (!session || !user || (session.user.id && user.id !== session.user.id)) return;
      session = { ...session, user: { ...session.user, ...user } };
      try { globalThis.localStorage?.setItem(SESSION_KEY, JSON.stringify(session)); } catch { /* Storage may be unavailable in private mode. */ }
      draw();
    },
  };
}

function element(documentRef, tag, className, text) {
  const node = documentRef.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function money(value) {
  const amount = Number(value || 0);
  const minimumFractionDigits = Number.isInteger(amount) ? 0 : 2;
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', minimumFractionDigits, maximumFractionDigits: 2,
  }).format(amount);
}

function setFeedback(feedback, message) {
  if (feedback) feedback.textContent = message;
}

function reservationCard(documentRef, reservation, cancelContext) {
  const card = element(documentRef, 'article', 'reservation-card');
  card.append(element(documentRef, 'h3', '', reservation.listingTitle));
  card.append(element(documentRef, 'p', '', `${reservation.checkIn} al ${reservation.checkOut} · ${reservation.nights} noches`));
  card.append(element(documentRef, 'p', 'reservation-total', `Total: ${money(reservation.total)} COP`));
  card.append(element(documentRef, 'p', '', `Estado: ${reservation.status} · Pago: ${reservation.paymentStatus.replaceAll('_', ' ').toLowerCase()}`));
  if (reservation.status === 'CANCELLED') {
    card.append(element(documentRef, 'p', '', `Reembolso simulado: ${reservation.refundPercentage}% · ${money(reservation.refundAmount)} COP`));
  }
  if (reservation.status === 'CONFIRMED') card.append(cancellationForm(documentRef, reservation, cancelContext));
  if (reservation.status === 'COMPLETED') {
    if (reservation.reviewed) {
      card.append(element(documentRef, 'p', '', 'Ya escribiste una reseña para esta reserva.'));
    } else {
      const { apiBase, token, fetchImpl, callbacks } = cancelContext;
      initReviewForm(card, reservation, apiBase, callbacks.getToken || (() => token),
        () => callbacks.onReviewCreated?.(reservation), documentRef, fetchImpl);
    }
  }
  return card;
}

function cancellationForm(documentRef, reservation, cancelContext) {
  const form = element(documentRef, 'form', 'cancel-form');
  const label = element(documentRef, 'label', '', 'Motivo de cancelación');
  const reason = element(documentRef, 'textarea');
  reason.name = 'reason'; reason.required = true; reason.maxLength = 1000; reason.rows = 2;
  label.append(reason);
  const button = element(documentRef, 'button', 'secondary-button', 'Cancelar reserva');
  button.type = 'submit';
  form.append(label, button);
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    await cancelReservation(reservation, reason, button, cancelContext);
  });
  return form;
}

async function cancelReservation(reservation, reason, button, context) {
  const { apiBase, token, fetchImpl, documentRef, container, feedback } = context;
  button.disabled = true;
  setFeedback(feedback, 'Procesando cancelación…');
  try {
    const response = await fetchImpl(new URL(`/api/reservations/${encodeURIComponent(reservation.id)}/cancel`, apiBase), {
      method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: reason.value }),
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'No fue posible cancelar la reserva.');
    if (context.callbacks.getToken && context.callbacks.getToken() !== token) return;
    await renderReservationHistory(container, apiBase, token, fetchImpl, documentRef,
      `Cancelación registrada. Reembolso de prueba: ${result.refundPercentage}% (${money(result.refundAmount)} COP).`,
      context.callbacks);
    context.callbacks.onReservationChanged?.();
  } catch (error) {
    if (context.callbacks.getToken && context.callbacks.getToken() !== token) return;
    button.disabled = false;
    setFeedback(feedback, error.message);
  }
}

function renderReservationCards(items, documentRef, reservations, cancelContext) {
  items.replaceChildren();
  if (!reservations.length) items.append(element(documentRef, 'p', '', 'Aún no tienes reservas.'));
  for (const reservation of reservations) items.append(reservationCard(documentRef, reservation, cancelContext));
}

export async function renderReservationHistory(container, apiBase, token, fetchImpl = globalThis.fetch,
  documentRef = globalThis.document, notice = '', callbacks = {}) {
  if (!container) return;
  const request = (historyRequests.get(container) || 0) + 1;
  historyRequests.set(container, request);
  container.hidden = !token;
  const feedback = documentRef.querySelector('#reservation-history-feedback');
  const items = documentRef.querySelector('#reservation-history-items');
  if (!token) {
    items?.replaceChildren();
    setFeedback(feedback, '');
    return;
  }
  if (!items) return;
  items.replaceChildren();
  setFeedback(feedback, 'Cargando tus reservas…');
  try {
    const response = await fetchImpl(new URL('/api/reservations', apiBase), {
      headers: { Authorization: `Bearer ${token}` },
    });
    const reservations = await response.json().catch(() => []);
    if (historyRequests.get(container) !== request) return;
    if (!response.ok) throw new Error(reservations.message || 'No fue posible consultar tus reservas.');
    const cancelContext = { apiBase, token, fetchImpl, documentRef, container, feedback, callbacks };
    renderReservationCards(items, documentRef, reservations, cancelContext);
    setFeedback(feedback, notice || (reservations.length ? '' : 'Aún no tienes reservas.'));
  } catch (error) {
    if (historyRequests.get(container) === request) setFeedback(feedback, error.message);
  }
}
