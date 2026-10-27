const SESSION_KEY = 'marketplace-session';

function readSession() {
  try {
    const session = JSON.parse(globalThis.localStorage?.getItem(SESSION_KEY) || 'null');
    if (!session?.accessToken || !session?.user || Date.parse(session.expiresAt) <= Date.now()) {
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

export function initAccount(documentRef = globalThis.document, apiBase = 'http://localhost:8080', onSessionChange = () => {}, fetchImpl = globalThis.fetch) {
  let session = readSession();
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

  function formValues(form) {
    return Object.fromEntries(new FormData(form).entries());
  }

  async function authenticate(form, path) {
    if (!form || typeof FormData === 'undefined') return;
    if (feedback) feedback.textContent = 'Procesando…';
    try {
      session = await sendAuthRequest(path, formValues(form), apiBase, fetchImpl);
      try { globalThis.localStorage?.setItem(SESSION_KEY, JSON.stringify(session)); } catch { /* Storage may be unavailable in private mode. */ }
      form.reset();
      draw(path === 'register' ? 'Cuenta creada e ingreso completado.' : 'Sesión iniciada.');
    } catch (error) {
      if (feedback) feedback.textContent = error.message || 'No fue posible iniciar sesión.';
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
  logoutButton?.addEventListener?.('click', () => {
    session = null;
    try { globalThis.localStorage?.removeItem(SESSION_KEY); } catch { /* Storage may be unavailable in private mode. */ }
    draw('Sesión cerrada.');
  });

  draw();
  return { getToken: () => session?.accessToken || null, getSession: () => session };
}

function element(documentRef, tag, className, text) {
  const node = documentRef.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function money(value) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(value || 0));
}

export async function renderReservationHistory(container, apiBase, token, fetchImpl = globalThis.fetch,
  documentRef = globalThis.document, notice = '') {
  if (!container) return;
  container.hidden = !token;
  const feedback = documentRef.querySelector('#reservation-history-feedback');
  const items = documentRef.querySelector('#reservation-history-items');
  if (!token || !items) return;
  if (feedback) feedback.textContent = 'Cargando tus reservas…';
  try {
    const response = await fetchImpl(new URL('/api/reservations', apiBase), {
      headers: { Authorization: `Bearer ${token}` },
    });
    const reservations = await response.json().catch(() => []);
    if (!response.ok) throw new Error(reservations.message || 'No fue posible consultar tus reservas.');
    items.replaceChildren();
    if (!reservations.length) items.append(element(documentRef, 'p', '', 'Aún no tienes reservas.'));
    for (const reservation of reservations) {
      const card = element(documentRef, 'article', 'reservation-card');
      card.append(element(documentRef, 'h3', '', reservation.listingTitle));
      card.append(element(documentRef, 'p', '', `${reservation.checkIn} al ${reservation.checkOut} · ${reservation.nights} noches`));
      card.append(element(documentRef, 'p', 'reservation-total', `Total: ${money(reservation.total)} COP`));
      card.append(element(documentRef, 'p', '', `Estado: ${reservation.status} · Pago: ${reservation.paymentStatus.replaceAll('_', ' ').toLowerCase()}`));
      if (reservation.status === 'CANCELLED') {
        card.append(element(documentRef, 'p', '', `Reembolso simulado: ${reservation.refundPercentage}% · ${money(reservation.refundAmount)} COP`));
      }
      if (reservation.status === 'CONFIRMED') {
        const cancelForm = element(documentRef, 'form', 'cancel-form');
        const label = element(documentRef, 'label', '', 'Motivo de cancelación');
        const reason = element(documentRef, 'textarea');
        reason.name = 'reason'; reason.required = true; reason.maxLength = 1000; reason.rows = 2;
        label.append(reason);
        const button = element(documentRef, 'button', 'secondary-button', 'Cancelar reserva');
        button.type = 'submit';
        cancelForm.append(label, button);
        cancelForm.addEventListener('submit', async (event) => {
          event.preventDefault();
          button.disabled = true;
          if (feedback) feedback.textContent = 'Procesando cancelación…';
          try {
            const cancelResponse = await fetchImpl(new URL(`/api/reservations/${encodeURIComponent(reservation.id)}/cancel`, apiBase), {
              method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
              body: JSON.stringify({ reason: reason.value }),
            });
            const result = await cancelResponse.json().catch(() => ({}));
            if (!cancelResponse.ok) throw new Error(result.message || 'No fue posible cancelar la reserva.');
            await renderReservationHistory(container, apiBase, token, fetchImpl, documentRef,
              `Cancelación registrada. Reembolso de prueba: ${result.refundPercentage}% (${money(result.refundAmount)} COP).`);
          } catch (error) {
            button.disabled = false;
            if (feedback) feedback.textContent = error.message;
          }
        });
        card.append(cancelForm);
      }
      items.append(card);
    }
    if (feedback) feedback.textContent = notice || (reservations.length ? '' : 'Aún no tienes reservas.');
  } catch (error) {
    if (feedback) feedback.textContent = error.message;
  }
}
