import { createBookingProgress } from './booking-progress.js';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Never navigate to a URL merely because it came back from the API.
export function stripeCheckoutUrl(value) {
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:' || url.hostname !== 'checkout.stripe.com'
        || url.username || url.password || (url.port && url.port !== '443')) return null;
    return url.href;
  } catch { return null; }
}

export async function fetchPaymentMode(apiBase, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(new URL('/api/payments/config', apiBase));
  if (!response.ok) throw new Error('No fue posible consultar las opciones de pago.');
  const data = await response.json();
  if (!['SIMULATED_TEST', 'STRIPE_TEST'].includes(data.mode)) throw new Error('El modo de pago no está disponible.');
  return data.mode;
}

/** The URL only identifies an attempt; the server determines its status. */
export function initStripeReturn(container, attemptId, apiBase, getToken,
  onConfirmed = () => {}, doc = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container || !attemptId) return null;
  container.hidden = false;
  const heading = doc.createElement('h2'); heading.textContent = 'Estado del pago de prueba';
  const message = doc.createElement('p'); message.setAttribute('role', 'status'); message.setAttribute('aria-live', 'polite');
  const retry = doc.createElement('button'); retry.type = 'button'; retry.className = 'secondary-button'; retry.textContent = 'Consultar estado';
  const resume = doc.createElement('a'); resume.className = 'listing-detail-link'; resume.textContent = 'Continuar pago de prueba'; resume.hidden = true;
  const progress = createBookingProgress(doc, 'STRIPE_TEST');
  progress.setStep(2, 'STRIPE_TEST');
  container.replaceChildren(heading, message, retry, resume, progress.element);
  let revision = 0;
  let confirmedToken = null;
  async function refresh() {
    const request = ++revision;
    const token = getToken();
    progress.setStep(2, 'STRIPE_TEST');
    message.setAttribute('data-status', 'loading');
    resume.hidden = true;
    resume.removeAttribute?.('href');
    if (!UUID.test(attemptId)) { message.setAttribute('data-status', 'error'); message.textContent = 'El identificador del pago no es válido. Consulta tus reservas o inicia una nueva búsqueda.'; retry.hidden = true; return; }
    if (!token) { message.setAttribute('data-status', 'idle'); message.textContent = 'Inicia sesión con la cuenta que inició el pago para consultar su estado.'; retry.hidden = true; return; }
    retry.hidden = false; retry.disabled = true;
    message.textContent = 'Consultando el estado del pago de prueba…';
    try {
      const response = await fetchImpl(new URL(`/api/stripe-test/checkouts/${encodeURIComponent(attemptId)}`, apiBase),
        { headers: { Authorization: `Bearer ${token}` } });
      const data = await response.json().catch(() => ({}));
      if (request !== revision || token !== getToken()) return;
      if (!response.ok) throw new Error(data.message || 'No fue posible consultar el pago.');
      if (data.attemptId !== attemptId) throw new Error('La respuesta del pago no corresponde a la solicitud.');
      message.setAttribute('data-status', 'idle');
      switch (data.status) {
        case 'PAID':
          progress.setStep(3, 'STRIPE_TEST');
          message.setAttribute('data-status', 'success');
          message.textContent = 'Pago de prueba confirmado por el servidor. La reserva está confirmada.';
          if (confirmedToken !== token) { confirmedToken = token; onConfirmed(); }
          break;
        case 'PREPARING': message.textContent = 'El pago de prueba se está preparando. Consulta nuevamente en unos momentos.'; break;
        case 'OPEN': {
          message.textContent = 'El pago de prueba está pendiente. La reserva aún no está confirmada.';
          const url = stripeCheckoutUrl(data.checkoutUrl);
          if (url) { resume.href = url; resume.hidden = false; }
          break;
        }
        case 'EXPIRED': message.textContent = 'El pago de prueba venció. La reserva no se confirmó. Busca nuevas fechas para intentarlo de nuevo.'; break;
        case 'REFUND_PENDING': message.textContent = 'El pago de prueba está en revisión o pendiente de reembolso. Consulta tu historial de reservas.'; break;
        case 'REFUND_FAILED': message.textContent = 'El reembolso de prueba falló y requiere revisión. Consulta tu historial de reservas.'; break;
        case 'REFUND_REVIEW': message.textContent = 'El estado del reembolso de prueba requiere revisión. Consulta tu historial de reservas.'; break;
        case 'REFUNDED': message.textContent = 'El pago de prueba fue reembolsado. Consulta tu historial de reservas.'; break;
        case 'CANCELLED': message.textContent = 'La reserva fue cancelada sin reembolso según la política aplicable.'; break;
        default: message.textContent = 'El estado del pago no está disponible. Consulta nuevamente.';
      }
    } catch (error) {
      if (request === revision && token === getToken()) {
        message.setAttribute('data-status', 'error');
        message.textContent = `${error.message || 'No fue posible consultar el pago.'} Puedes consultar el estado de nuevo.`;
      }
    } finally {
      if (request === revision && token === getToken()) retry.disabled = false;
    }
  }
  retry.addEventListener('click', () => { void refresh(); });
  return { refresh };
}
