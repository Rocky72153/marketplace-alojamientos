import { stripeCheckoutUrl } from './stripe-checkout.js';
import { createBookingProgress } from './booking-progress.js';

function el(documentRef, tag, className, text) {
  const node = documentRef.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function formatMoney(value) {
  const amount = Number(value || 0);
  const minimumFractionDigits = Number.isInteger(amount) ? 0 : 2;
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', minimumFractionDigits, maximumFractionDigits: 2,
  }).format(amount);
}

function bogotaToday() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}-${values.month}-${values.day}`;
}

function laterDate(days) {
  const date = new Date(`${bogotaToday()}T12:00:00-05:00`);
  date.setDate(date.getDate() + days);
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date);
}

function makeIdempotencyKey() {
  const cryptoApi = globalThis.crypto;
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
  if (typeof cryptoApi?.getRandomValues === 'function') {
    const values = cryptoApi.getRandomValues(new Uint32Array(4));
    return `booking-${Array.from(values, (value) => value.toString(16).padStart(8, '0')).join('')}`;
  }
  throw new Error('Tu navegador no puede generar una clave segura para la reserva.');
}

function validDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function initBooking(container, listing, apiBase, getToken, onReservationChanged = () => {},
  documentRef = globalThis.document, fetchImpl = globalThis.fetch, options = {}) {
  if (!container || !listing) return null;
  const section = el(documentRef, 'section', 'booking-panel');
  section.setAttribute?.('aria-labelledby', 'booking-title');
  const title = el(documentRef, 'h2', '', 'Cotiza y reserva');
  title.id = 'booking-title';
  title.setAttribute?.('tabindex', '-1');
  const progress = createBookingProgress(documentRef, options.getPaymentMode?.());
  section.append(title, el(documentRef, 'p', 'booking-intro', 'Elige tus fechas y revisa el total antes de continuar. El pago es de prueba.'), progress.element);

  const form = el(documentRef, 'form', 'booking-form');
  form.id = 'quote-form';
  const checkIn = el(documentRef, 'input');
  checkIn.type = 'date'; checkIn.name = 'checkIn'; checkIn.required = true; checkIn.min = bogotaToday(); checkIn.value = laterDate(7);
  const checkOut = el(documentRef, 'input');
  checkOut.type = 'date'; checkOut.name = 'checkOut'; checkOut.required = true; checkOut.min = checkIn.value; checkOut.value = laterDate(9);
  const guests = el(documentRef, 'input');
  guests.type = 'number'; guests.name = 'guests'; guests.required = true; guests.min = '1'; guests.max = String(listing.capacity); guests.value = String(Math.min(2, listing.capacity));
  const win = options.window || documentRef.defaultView || globalThis.window;
  const params = new URLSearchParams(win?.location?.search || '');
  const requestedCheckIn = params.get('checkin');
  const requestedCheckOut = params.get('checkout');
  const requestedGuests = params.get('guests');
  if (validDate(requestedCheckIn) && requestedCheckIn >= bogotaToday()) {
    checkIn.value = requestedCheckIn;
    checkOut.min = requestedCheckIn;
    if (checkOut.value <= requestedCheckIn) checkOut.value = '';
  }
  if (validDate(requestedCheckOut) && requestedCheckOut > checkIn.value) checkOut.value = requestedCheckOut;
  if (/^[1-9]\d*$/.test(requestedGuests || '') && Number(requestedGuests) <= Number(listing.capacity)) guests.value = requestedGuests;
  const fields = [
    ['Llegada', checkIn], ['Salida', checkOut], ['Huéspedes', guests],
  ];
  for (const [caption, input] of fields) {
    const label = el(documentRef, 'label', '', caption);
    label.append(input);
    form.append(label);
  }

  const extraInputs = [];
  const extras = Array.isArray(listing.extraOptions) ? listing.extraOptions : [];
  if (extras.length) {
    const fieldset = el(documentRef, 'fieldset', 'booking-extras');
    fieldset.append(el(documentRef, 'legend', '', 'Extras opcionales'));
    for (const extra of extras) {
      const row = el(documentRef, 'div', 'booking-extra-row');
      const checkbox = el(documentRef, 'input');
      checkbox.type = 'checkbox'; checkbox.value = extra.id;
      const checkboxLabel = el(documentRef, 'label', '', `${extra.name} · ${formatMoney(extra.unitPrice)}`);
      checkboxLabel.prepend(checkbox);
      const quantity = el(documentRef, 'input');
      quantity.type = 'number'; quantity.min = '1'; quantity.max = '99'; quantity.step = '1'; quantity.value = '1';
      quantity.disabled = true; quantity.setAttribute?.('aria-label', `Cantidad de ${extra.name}`);
      checkbox.addEventListener?.('change', () => { quantity.disabled = !checkbox.checked; });
      row.append(checkboxLabel, quantity);
      fieldset.append(row);
      extraInputs.push({ checkbox, quantity });
    }
    form.append(fieldset);
  } else {
    form.append(el(documentRef, 'p', 'booking-no-extras', 'Este alojamiento no ofrece extras pagos.'));
  }

  const quoteButton = el(documentRef, 'button', 'primary-button', 'Calcular cotización');
  quoteButton.type = 'submit';
  form.append(quoteButton);
  const feedback = el(documentRef, 'p', 'booking-feedback');
  feedback.role = 'status'; feedback.setAttribute?.('aria-live', 'polite');
  const quoteSummary = el(documentRef, 'div', 'quote-summary');
  quoteSummary.hidden = true;
  const staySummary = el(documentRef, 'p', 'booking-stay-summary');
  staySummary.hidden = true;
  const editQuoteButton = el(documentRef, 'button', 'secondary-button booking-edit', 'Cambiar fechas y extras');
  editQuoteButton.type = 'button'; editQuoteButton.hidden = true;
  const reserveButton = el(documentRef, 'button', 'primary-button reserve-button', 'Confirmar reserva · pago de prueba');
  reserveButton.type = 'button'; reserveButton.hidden = true;
  const paymentStatusLink = el(documentRef, 'a', 'listing-detail-link booking-payment-status', 'Consultar estado del pago');
  paymentStatusLink.hidden = true;
  section.append(form, staySummary, editQuoteButton, feedback, quoteSummary, reserveButton, paymentStatusLink);
  container.append(section);

  const current = { payload: null, idempotencyKey: null, total: null };
  let quoteRevision = 0;
  let accountRevision = 0;
  let accountToken = getToken();
  let reservationInFlight = false;
  const getPaymentMode = options.getPaymentMode || (() => 'SIMULATED_TEST');
  const blockedRole = () => Boolean(getToken() && options.getRole && options.getRole() !== 'GUEST');
  const navigate = options.navigate || ((url) => globalThis.location.assign(url));
  let progressStep = 0;
  function setStep(step) {
    progressStep = step;
    progress.setStep(step, getPaymentMode());
  }
  function clearQuote() {
    quoteRevision += 1;
    current.payload = null;
    current.idempotencyKey = null;
    current.total = null;
    setStep(0);
    feedback.setAttribute?.('data-status', 'idle');
    quoteButton.disabled = reservationInFlight;
    quoteSummary.replaceChildren();
    quoteSummary.hidden = true;
    reserveButton.hidden = true;
    paymentStatusLink.hidden = true;
    paymentStatusLink.removeAttribute?.('href');
    feedback.textContent = '';
    form.hidden = false;
    staySummary.hidden = true;
    editQuoteButton.hidden = true;
  }
  editQuoteButton.addEventListener?.('click', () => { clearQuote(); checkIn.focus?.(); });
  const updateCheckoutMinimum = () => {
    checkOut.min = checkIn.value || bogotaToday();
    if (checkOut.value && checkOut.value <= checkIn.value) checkOut.value = '';
  };
  for (const eventName of ['input', 'change']) {
    checkIn.addEventListener?.(eventName, () => { updateCheckoutMinimum(); clearQuote(); });
    checkOut.addEventListener?.(eventName, clearQuote);
    guests.addEventListener?.(eventName, clearQuote);
  }
  for (const item of extraInputs) {
    item.checkbox.addEventListener?.('change', clearQuote);
    item.quantity.addEventListener?.('input', clearQuote);
    item.quantity.addEventListener?.('change', clearQuote);
  }

  function selectedPayload() {
    return {
      checkIn: checkIn.value,
      checkOut: checkOut.value,
      guests: Number(guests.value),
      extras: extraInputs.filter(({ checkbox }) => checkbox.checked)
        .map(({ checkbox, quantity }) => ({ extraId: checkbox.value, quantity: Number(quantity.value) })),
    };
  }

  function renderQuote(quote) {
    form.hidden = true;
    staySummary.hidden = false;
    editQuoteButton.hidden = false;
    const dateLabel = (value) => new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', timeZone: 'America/Bogota' })
      .format(new Date(`${value}T12:00:00-05:00`));
    staySummary.textContent = `${dateLabel(current.payload.checkIn)} – ${dateLabel(current.payload.checkOut)} · ${current.payload.guests} huésped${current.payload.guests === 1 ? '' : 'es'}`;
    quoteSummary.replaceChildren();
    quoteSummary.hidden = false;
    quoteSummary.append(el(documentRef, 'h3', '', 'Desglose de la estadía'));
    const rows = [
      [`${quote.nights} noches · ${formatMoney(quote.pricePerNight)} por noche`, quote.nightlySubtotal],
      ['Limpieza', quote.cleaningFee], ['Servicio', quote.serviceFee],
      ['Descuentos', `−${formatMoney(quote.discountAmount)}`],
    ];
    for (const extra of quote.extras || []) rows.push([`${extra.name} × ${extra.quantity}`, extra.lineTotal]);
    for (const [labelText, amount] of rows) {
      const row = el(documentRef, 'div', 'quote-row');
      row.append(el(documentRef, 'span', '', labelText), el(documentRef, 'strong', '', typeof amount === 'string' ? amount : formatMoney(amount)));
      quoteSummary.append(row);
    }
    const total = el(documentRef, 'div', 'quote-total');
    total.append(el(documentRef, 'span', '', 'Total estimado'), el(documentRef, 'strong', '', formatMoney(quote.total)));
    quoteSummary.append(total);
    quoteSummary.append(el(documentRef, 'p', 'cancellation-policy',
      `Política del alojamiento: 100% desde ${quote.freeRefundDays} días; ${quote.partialRefundPercentage}% entre ${quote.partialRefundDays} y ${quote.freeRefundDays - 1} días; 0% con menos de ${quote.partialRefundDays} días.`));
    reserveButton.hidden = false;
    syncPaymentMode();
    if (!getToken()) feedback.textContent = 'Inicia sesión o crea una cuenta de huésped para confirmar la reserva.';
  }

  function syncPaymentMode() {
    const mode = getPaymentMode();
    progress.setStep(progressStep, mode);
    const amount = current.total == null ? '' : ` · ${formatMoney(current.total)}`;
    if (mode === 'STRIPE_TEST') reserveButton.textContent = getToken() ? `Continuar a Stripe de prueba${amount}` : 'Iniciar sesión para reservar';
    else if (mode === 'SIMULATED_TEST') reserveButton.textContent = getToken() ? `Confirmar reserva de prueba${amount}` : 'Iniciar sesión para reservar';
    else reserveButton.textContent = 'Opciones de pago no disponibles';
    reserveButton.disabled = reservationInFlight || blockedRole() || !['STRIPE_TEST', 'SIMULATED_TEST'].includes(mode);
    if (mode === 'UNAVAILABLE' && current.payload) feedback.textContent = 'No fue posible consultar las opciones de pago. Recarga la página para reintentar.';
    if (blockedRole()) {
      reserveButton.textContent = 'Reservas disponibles para huéspedes';
      feedback.textContent = 'Para reservar, usa una cuenta de huésped. Con tu cuenta actual puedes explorar y cotizar.';
    }
  }

  form.addEventListener?.('submit', async (event) => {
    event.preventDefault();
    updateCheckoutMinimum();
    if (!form.reportValidity?.()) return;
    clearQuote();
    const revision = quoteRevision;
    setStep(1);
    feedback.setAttribute?.('data-status', 'loading');
    quoteButton.disabled = true;
    feedback.textContent = 'Calculando con precios y disponibilidad actuales…';
    const payload = selectedPayload();
    try {
      const response = await fetchImpl(new URL(`/api/listings/${encodeURIComponent(listing.id)}/quotes`, apiBase), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
      });
      const result = await response.json().catch(() => ({}));
      if (revision !== quoteRevision) return;
      if (!response.ok) throw new Error(result.message || 'No fue posible cotizar estas fechas.');
      current.payload = payload;
      current.idempotencyKey = makeIdempotencyKey();
      current.total = result.total;
      feedback.setAttribute?.('data-status', 'success');
      feedback.textContent = 'Cotización actualizada. Confirma para bloquear las fechas.';
      renderQuote(result);
    } catch (error) {
      if (revision === quoteRevision) {
        feedback.setAttribute?.('data-status', 'error');
        feedback.textContent = `${error.message || 'No fue posible cotizar estas fechas.'} Revisa los datos y vuelve a calcular.`;
      }
    } finally {
      if (revision === quoteRevision) quoteButton.disabled = false;
    }
  });

  reserveButton.addEventListener?.('click', async () => {
    if (reservationInFlight) return;
    const token = getToken();
    if (blockedRole()) { syncPaymentMode(); return; }
    if (!token) {
      feedback.textContent = 'Inicia sesión como huésped para confirmar la reserva.';
      if (win?.location?.href && win?.history?.replaceState) {
        const url = new URL(win.location.href);
        url.searchParams.set('checkin', checkIn.value);
        url.searchParams.set('checkout', checkOut.value);
        url.searchParams.set('guests', guests.value);
        win.history.replaceState(win.history.state, '', url);
      }
      if (options.onRequireAccount) { options.onRequireAccount(); return; }
      const accountPanel = documentRef.querySelector('#account-panel');
      if (accountPanel) accountPanel.hidden = false;
      documentRef.querySelector('#account-panel')?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      return;
    }
    if (!current.payload || !current.idempotencyKey) return;
    const mode = getPaymentMode();
    if (!['SIMULATED_TEST', 'STRIPE_TEST'].includes(mode)) {
      feedback.textContent = 'No fue posible consultar las opciones de pago. Recarga la página para reintentar.';
      return;
    }
    const revision = accountRevision;
    reservationInFlight = true;
    setStep(2);
    feedback.setAttribute?.('data-status', 'loading');
    const controls = [checkIn, checkOut, guests, quoteButton, editQuoteButton, ...extraInputs.flatMap(({ checkbox, quantity }) => [checkbox, quantity])];
    const disabledBeforeReservation = controls.map((control) => control.disabled);
    controls.forEach((control) => { control.disabled = true; });
    reserveButton.disabled = true;
    feedback.textContent = mode === 'STRIPE_TEST' ? 'Preparando el pago de prueba en Stripe…' : 'Confirmando reserva con pago simulado…';
    const requestBody = { listingId: listing.id, ...current.payload };
    try {
      const response = await fetchImpl(new URL(mode === 'STRIPE_TEST' ? '/api/stripe-test/checkouts' : '/api/reservations', apiBase), {
        method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
          'Idempotency-Key': current.idempotencyKey }, body: JSON.stringify(requestBody),
      });
      const result = await response.json().catch(() => ({}));
      if (revision !== accountRevision || token !== getToken()) return;
      if (!response.ok) throw new Error(result.message || 'No fue posible confirmar la reserva.');
      if (mode === 'STRIPE_TEST') {
        if (result.status === 'PREPARING') {
          feedback.setAttribute?.('data-status', 'idle');
          feedback.textContent = 'El pago de prueba se está preparando. Vuelve a intentarlo para continuar.';
          return;
        }
        if (result.status !== 'OPEN') {
          feedback.setAttribute?.('data-status', 'idle');
          if (/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(result.attemptId || '')) {
            paymentStatusLink.href = `?stripe_checkout=${encodeURIComponent(result.attemptId)}#pago`;
            paymentStatusLink.hidden = false;
            feedback.textContent = 'Consulta el estado del pago. La reserva aún no se confirma desde esta pantalla.';
          } else {
            feedback.textContent = 'Consulta tus reservas para conocer el estado. La reserva aún no se confirma desde esta pantalla.';
          }
          return;
        }
        const url = stripeCheckoutUrl(result.checkoutUrl);
        if (!url) throw new Error('No fue posible abrir una página de pago válida de Stripe.');
        feedback.textContent = 'Abriendo Stripe en modo de prueba. La reserva se confirmará cuando el servidor verifique el pago.';
        navigate(url);
        return;
      }
      current.payload = null; current.idempotencyKey = null;
      reserveButton.hidden = true;
      setStep(3);
      feedback.setAttribute?.('data-status', 'success');
      feedback.textContent = `Reserva confirmada. Código ${result.id}. Pago simulado aprobado; no se realizó ningún cobro real.`;
      try {
        await Promise.resolve(onReservationChanged());
      } catch {
        if (revision === accountRevision && token === getToken()) {
          feedback.textContent = `Reserva confirmada. Código ${result.id}. Pago simulado aprobado; no se realizó ningún cobro real. No fue posible actualizar el historial.`;
        }
      }
    } catch (error) {
      if (revision === accountRevision && token === getToken()) {
        feedback.setAttribute?.('data-status', 'error');
        feedback.textContent = `${error.message || 'No fue posible confirmar la reserva.'} Puedes intentarlo de nuevo.`;
      }
    } finally {
      reservationInFlight = false;
      controls.forEach((control, index) => { control.disabled = disabledBeforeReservation[index]; });
      if (current.payload) syncPaymentMode();
    }
  });
  return {
    section, form, quoteButton, reserveButton,
    syncAccount() {
      const nextToken = getToken();
      if (nextToken !== accountToken) {
        accountToken = nextToken;
        accountRevision += 1;
        if (reservationInFlight) clearQuote();
        else if (current.payload) feedback.textContent = nextToken
          ? 'Sesión iniciada. Revisa la cotización antes de continuar.'
          : 'Inicia sesión o crea una cuenta de huésped para confirmar la reserva.';
      }
      if (!current.payload) return;
      syncPaymentMode();
    },
    syncPaymentMode,
  };
}
