function element(documentRef, tag, className, text) {
  const node = documentRef.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function dateLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Fecha no disponible';
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function initNotifications(container, apiBase, getToken, fetchImpl = globalThis.fetch,
  documentRef = globalThis.document) {
  const items = documentRef?.querySelector('#notifications-items');
  const feedback = documentRef?.querySelector('#notifications-feedback');
  const link = documentRef?.querySelector('#notifications-link');
  const accountPanel = documentRef?.querySelector('#account-panel');
  const accountFeedback = documentRef?.querySelector('#account-feedback');
  let sequence = 0;

  function clear() {
    sequence += 1;
    if (container) container.hidden = true;
    items?.replaceChildren();
    if (feedback) feedback.textContent = '';
  }

  const isCurrent = (token, request) => request === sequence && token === getToken();

  async function markRead(notification, button, token, request) {
    button.disabled = true;
    try {
      const response = await fetchImpl(new URL(`/api/notifications/${encodeURIComponent(notification.id)}/read`, apiBase), {
        method: 'PATCH', headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || 'No fue posible actualizar la notificación.');
      }
      if (isCurrent(token, request)) await refresh();
    } catch (error) {
      if (isCurrent(token, request)) {
        button.disabled = false;
        if (feedback) feedback.textContent = error.message || 'No fue posible actualizar la notificación.';
      }
    }
  }

  function card(notification, token, request) {
    const article = element(documentRef, 'article', 'notification-card');
    article.append(element(documentRef, 'p', 'notification-message', notification.message || 'Notificación'));
    const state = notification.readAt ? 'Leída' : 'Sin leer';
    article.append(element(documentRef, 'p', 'notification-meta', `${dateLabel(notification.createdAt)} · ${state}`));
    if (!notification.readAt) {
      const button = element(documentRef, 'button', 'secondary-button notification-read-button', 'Marcar como leída');
      button.type = 'button';
      button.addEventListener('click', () => markRead(notification, button, token, request));
      article.append(button);
    }
    return article;
  }

  async function refresh() {
    const token = getToken();
    if (!token) {
      clear();
      return;
    }
    if (!container || !items || !feedback) return;
    const request = ++sequence;
    container.hidden = false;
    feedback.textContent = 'Cargando notificaciones…';
    items.replaceChildren();
    try {
      const response = await fetchImpl(new URL('/api/notifications', apiBase), {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new Error(data?.message || 'No fue posible consultar tus notificaciones.');
      if (!Array.isArray(data)) throw new Error('No fue posible consultar tus notificaciones.');
      if (!isCurrent(token, request)) return;
      if (!data.length) items.append(element(documentRef, 'p', 'notification-empty', 'Aún no tienes notificaciones.'));
      for (const notification of data) items.append(card(notification, token, request));
      feedback.textContent = '';
    } catch (error) {
      if (isCurrent(token, request)) feedback.textContent = error.message || 'No fue posible consultar tus notificaciones.';
    }
  }

  link?.addEventListener?.('click', (event) => {
    event.preventDefault();
    if (!getToken()) {
      if (accountPanel) accountPanel.hidden = false;
      if (accountFeedback) accountFeedback.textContent = 'Inicia sesión para consultar tus notificaciones.';
      accountPanel?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      return;
    }
    void refresh();
    container?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  });

  return { refresh, clear };
}
