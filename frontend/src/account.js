import { initReviewForm } from './reviews.js';

const SESSION_KEY = 'marketplace-session';
const historyRequests = new WeakMap();
const historyViews = new WeakMap();

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
  const pendingForms = new WeakMap();
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

  function finishForm(form, revision) {
    const pending = pendingForms.get(form);
    if (!pending || (revision !== undefined && pending.revision !== revision)) return;
    if (pending.button) { pending.button.disabled = false; pending.button.textContent = pending.label; }
    form.setAttribute?.('aria-busy', 'false');
    pendingForms.delete(form);
  }

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
    const submitButton = form.querySelector?.('button[type="submit"]');
    if (path === 'register') {
      registrationPending = true;
    }
    const revision = ++authenticationRevision;
    const label = pendingForms.get(form)?.label ?? submitButton?.textContent;
    pendingForms.set(form, { revision, button: submitButton, label });
    if (submitButton) { submitButton.disabled = true; submitButton.textContent = path === 'register' ? 'Creando cuenta…' : 'Iniciando sesión…'; }
    form.setAttribute?.('aria-busy', 'true');
    if (feedback) { feedback.textContent = 'Procesando…'; feedback.setAttribute?.('data-status', 'loading'); }
    try {
      const nextSession = await sendAuthRequest(path, formValues(form), apiBase, fetchImpl);
      if (revision !== authenticationRevision) return;
      session = nextSession;
      try { globalThis.localStorage?.setItem(SESSION_KEY, JSON.stringify(session)); } catch { /* Storage may be unavailable in private mode. */ }
      form.reset();
      draw(path === 'register' ? 'Cuenta creada e ingreso completado.' : 'Sesión iniciada.');
      feedback?.setAttribute?.('data-status', 'success');
    } catch (error) {
      if (revision === authenticationRevision && feedback) {
        feedback.textContent = error.message || 'No fue posible iniciar sesión.';
        feedback.setAttribute?.('data-status', 'error');
      }
    } finally {
      if (path === 'register' && pendingForms.get(form)?.revision === revision) registrationPending = false;
      finishForm(form, revision);
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
    if (loginForm) finishForm(loginForm);
    if (registerForm) finishForm(registerForm);
    registrationPending = false;
    session = null;
    try { globalThis.localStorage?.removeItem(SESSION_KEY); } catch { /* Storage may be unavailable in private mode. */ }
    draw(message);
    feedback?.setAttribute?.('data-status', 'idle');
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

const RESERVATION_STATES = {
  PENDING_PAYMENT: 'pendiente de pago', CONFIRMED: 'confirmada', CANCELLED: 'cancelada', COMPLETED: 'completada',
};
const PAYMENT_STATES = {
  TEST_PENDING: 'pago de prueba pendiente',
  TEST_APPROVED: 'pago de prueba aprobado',
  TEST_EXPIRED: 'pago de prueba vencido',
  TEST_NO_REFUND: 'sin reembolso de prueba',
  TEST_REFUND_PENDING: 'reembolso de prueba pendiente',
  TEST_REFUND_REVIEW: 'reembolso de prueba requiere revisión',
  TEST_REFUND_FAILED: 'reembolso de prueba fallido; requiere revisión',
  TEST_REFUNDED: 'reembolso de prueba confirmado',
};

function reservationCard(documentRef, reservation, cancelContext) {
  const card = element(documentRef, 'article', 'reservation-card');
  card.append(element(documentRef, 'h3', '', reservation.listingTitle));
  card.append(element(documentRef, 'p', '', `${reservation.checkIn} al ${reservation.checkOut} · ${reservation.nights} noches`));
  card.append(element(documentRef, 'p', 'reservation-total', `Total: ${money(reservation.total)} COP`));
  const status = RESERVATION_STATES[reservation.status] || 'no disponible';
  const payment = PAYMENT_STATES[reservation.paymentStatus] || 'estado de pago no disponible';
  card.append(element(documentRef, 'span', `status-badge status-${String(reservation.status || 'unknown').toLowerCase()}`, status));
  card.append(element(documentRef, 'p', 'reservation-payment', payment));
  card.append(element(documentRef, 'p', 'reservation-reference', `Reserva: ${reservation.id}`));
  if (reservation.status === 'CANCELLED' && reservation.refundPercentage != null && reservation.refundAmount != null) {
    card.append(element(documentRef, 'p', '', `Reembolso de prueba: ${reservation.refundPercentage}% · ${money(reservation.refundAmount)} COP`));
  }
  if (reservation.status === 'CONFIRMED') {
    const action = element(documentRef, 'details', 'action-disclosure');
    action.append(element(documentRef, 'summary', '', 'Cancelar reserva'), cancellationForm(documentRef, reservation, cancelContext));
    card.append(action);
  }
  if (reservation.status === 'COMPLETED') {
    if (reservation.reviewed) {
      card.append(element(documentRef, 'p', '', 'Ya escribiste una reseña para esta reserva.'));
    } else {
      const { apiBase, token, fetchImpl, callbacks } = cancelContext;
      const action = element(documentRef, 'details', 'action-disclosure');
      action.append(element(documentRef, 'summary', '', 'Escribir una reseña'));
      card.append(action);
      initReviewForm(action, reservation, apiBase, callbacks.getToken || (() => token),
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
    const payment = PAYMENT_STATES[result.paymentStatus];
    await renderReservationHistory(container, apiBase, token, fetchImpl, documentRef,
      `Cancelación registrada. ${payment ? `${payment}. ` : ''}Reembolso de prueba: ${result.refundPercentage}% (${money(result.refundAmount)} COP).`,
      context.callbacks);
    context.callbacks.onReservationChanged?.();
  } catch (error) {
    if (context.callbacks.getToken && context.callbacks.getToken() !== token) return;
    button.disabled = false;
    setFeedback(feedback, error.message);
  }
}

function renderReservationCards(items, documentRef, reservations, cancelContext, emptyMessage = 'Aún no tienes reservas.') {
  items.replaceChildren();
  if (!reservations.length) items.append(element(documentRef, 'p', 'empty-state', emptyMessage));
  for (const reservation of reservations) items.append(reservationCard(documentRef, reservation, cancelContext));
}

function historyView(container, documentRef, feedback) {
  if (historyViews.has(container)) return historyViews.get(container);
  const toolbar = element(documentRef, 'div', 'workspace-toolbar reservation-filters');
  const queryLabel = element(documentRef, 'label', '', 'Buscar reserva');
  const query = element(documentRef, 'input'); query.type = 'search'; query.name = 'reservationQuery';
  query.placeholder = 'Alojamiento o código de reserva'; queryLabel.append(query);
  const stateLabel = element(documentRef, 'label', '', 'Estado');
  const state = element(documentRef, 'select'); state.name = 'reservationStatus';
  for (const [value, label] of [['', 'Todos los estados'], ...Object.entries(RESERVATION_STATES)]) {
    const option = element(documentRef, 'option', '', value ? label.charAt(0).toUpperCase() + label.slice(1) : label);
    option.value = value; state.append(option);
  }
  state.value = '';
  const count = element(documentRef, 'p', 'workspace-count'); count.setAttribute?.('role', 'status');
  toolbar.append(queryLabel, stateLabel, count); stateLabel.append(state);
  if (feedback?.before) feedback.before(toolbar); else container.append(toolbar);
  const view = { toolbar, query, state, count, token: null, draw: () => {} };
  query.addEventListener('input', () => view.draw());
  state.addEventListener('change', () => view.draw());
  historyViews.set(container, view);
  return view;
}

function normalized(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }

export async function renderReservationHistory(container, apiBase, token, fetchImpl = globalThis.fetch,
  documentRef = globalThis.document, notice = '', callbacks = {}) {
  if (!container) return;
  const request = (historyRequests.get(container) || 0) + 1;
  historyRequests.set(container, request);
  container.hidden = !token;
  const feedback = documentRef.querySelector('#reservation-history-feedback');
  const items = documentRef.querySelector('#reservation-history-items');
  const view = historyView(container, documentRef, feedback);
  view.toolbar.hidden = !token;
  view.draw = () => {};
  view.count.textContent = '';
  if (view.token !== token) { view.query.value = ''; view.state.value = ''; view.token = token; }
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
    if (!Array.isArray(reservations)) throw new Error('No fue posible consultar tus reservas.');
    const cancelContext = { apiBase, token, fetchImpl, documentRef, container, feedback, callbacks };
    view.draw = () => {
      if (historyRequests.get(container) !== request) return;
      const term = normalized(view.query.value).trim();
      const selected = reservations.filter((reservation) => (!view.state.value || reservation.status === view.state.value)
        && normalized(`${reservation.listingTitle} ${reservation.id}`).includes(term));
      view.count.textContent = `${selected.length} de ${reservations.length} reservas`;
      renderReservationCards(items, documentRef, selected, cancelContext,
        reservations.length ? 'No hay reservas que coincidan con estos filtros.' : 'Aún no tienes reservas.');
    };
    view.draw();
    setFeedback(feedback, notice || (reservations.length ? '' : 'Aún no tienes reservas.'));
  } catch (error) {
    if (historyRequests.get(container) === request) setFeedback(feedback, error.message);
  }
}
