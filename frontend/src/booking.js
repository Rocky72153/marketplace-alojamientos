function el(documentRef, tag, className, text) {
  const node = documentRef.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function formatMoney(value) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(value || 0));
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
  return globalThis.crypto?.randomUUID?.() || `booking-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export function initBooking(container, listing, apiBase, getToken, onReservationChanged = () => {},
  documentRef = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container || !listing) return null;
  const section = el(documentRef, 'section', 'booking-panel');
  section.setAttribute?.('aria-labelledby', 'booking-title');
  const title = el(documentRef, 'h2', '', 'Cotiza y reserva');
  title.id = 'booking-title';
  section.append(title, el(documentRef, 'p', 'booking-intro', 'Confirma primero las fechas. Los importes se recalculan con la tarifa y los extras vigentes del alojamiento.'));

  const form = el(documentRef, 'form', 'booking-form');
  form.id = 'quote-form';
  const checkIn = el(documentRef, 'input');
  checkIn.type = 'date'; checkIn.name = 'checkIn'; checkIn.required = true; checkIn.min = bogotaToday(); checkIn.value = laterDate(7);
  const checkOut = el(documentRef, 'input');
  checkOut.type = 'date'; checkOut.name = 'checkOut'; checkOut.required = true; checkOut.min = checkIn.value; checkOut.value = laterDate(9);
  const guests = el(documentRef, 'input');
  guests.type = 'number'; guests.name = 'guests'; guests.required = true; guests.min = '1'; guests.max = String(listing.capacity); guests.value = String(Math.min(2, listing.capacity));
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
  const reserveButton = el(documentRef, 'button', 'primary-button reserve-button', 'Confirmar reserva · pago de prueba');
  reserveButton.type = 'button'; reserveButton.hidden = true;
  section.append(form, feedback, quoteSummary, reserveButton);
  container.append(section);

  const current = { payload: null, idempotencyKey: null };
  function clearQuote() {
    current.payload = null;
    current.idempotencyKey = null;
    quoteSummary.replaceChildren();
    quoteSummary.hidden = true;
    reserveButton.hidden = true;
    feedback.textContent = '';
  }
  const updateCheckoutMinimum = () => {
    checkOut.min = checkIn.value || bogotaToday();
    if (checkOut.value && checkOut.value <= checkIn.value) checkOut.value = '';
  };
  checkIn.addEventListener?.('change', () => { updateCheckoutMinimum(); clearQuote(); });
  checkOut.addEventListener?.('change', clearQuote);
  guests.addEventListener?.('change', clearQuote);
  for (const item of extraInputs) {
    item.checkbox.addEventListener?.('change', clearQuote);
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
    reserveButton.textContent = getToken() ? 'Confirmar reserva · pago de prueba' : 'Iniciar sesión para reservar';
    reserveButton.disabled = false;
    if (!getToken()) feedback.textContent = 'Inicia sesión o crea una cuenta de huésped para confirmar la reserva.';
  }

  form.addEventListener?.('submit', async (event) => {
    event.preventDefault();
    updateCheckoutMinimum();
    if (!form.reportValidity?.()) return;
    clearQuote();
    quoteButton.disabled = true;
    feedback.textContent = 'Calculando con precios y disponibilidad actuales…';
    const payload = selectedPayload();
    try {
      const response = await fetchImpl(new URL(`/api/listings/${encodeURIComponent(listing.id)}/quotes`, apiBase), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || 'No fue posible cotizar estas fechas.');
      current.payload = payload;
      current.idempotencyKey = makeIdempotencyKey();
      feedback.textContent = 'Cotización actualizada. Confirma para bloquear las fechas.';
      renderQuote(result);
    } catch (error) {
      feedback.textContent = error.message || 'No fue posible cotizar estas fechas.';
    } finally {
      quoteButton.disabled = false;
    }
  });

  reserveButton.addEventListener?.('click', async () => {
    const token = getToken();
    if (!token) {
      feedback.textContent = 'Inicia sesión como huésped para confirmar la reserva.';
      const accountPanel = documentRef.querySelector('#account-panel');
      if (accountPanel) accountPanel.hidden = false;
      documentRef.querySelector('#account-panel')?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      return;
    }
    if (!current.payload || !current.idempotencyKey) return;
    reserveButton.disabled = true;
    feedback.textContent = 'Confirmando reserva con el proveedor de pago simulado…';
    const requestBody = { listingId: listing.id, ...current.payload };
    try {
      const response = await fetchImpl(new URL('/api/reservations', apiBase), {
        method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
          'Idempotency-Key': current.idempotencyKey }, body: JSON.stringify(requestBody),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || 'No fue posible confirmar la reserva.');
      current.payload = null; current.idempotencyKey = null;
      reserveButton.hidden = true;
      feedback.textContent = `Reserva confirmada. Código ${result.id}. Pago simulado aprobado; no se realizó ningún cobro real.`;
      await onReservationChanged();
    } catch (error) {
      feedback.textContent = error.message || 'No fue posible confirmar la reserva.';
      reserveButton.disabled = false;
    }
  });
  return {
    section, form, quoteButton, reserveButton,
    syncAccount() {
      if (!current.payload) return;
      reserveButton.textContent = getToken() ? 'Confirmar reserva · pago de prueba' : 'Iniciar sesión para reservar';
      reserveButton.disabled = false;
    },
  };
}
