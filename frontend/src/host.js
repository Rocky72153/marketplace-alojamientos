function element(doc, tag, text, className = '') {
  const node = doc.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function control(doc, form, label, name, type = 'text', value = '') {
  const wrapper = element(doc, 'label', label);
  const field = element(doc, type === 'textarea' ? 'textarea' : 'input');
  field.name = name;
  if (type !== 'textarea') field.type = type;
  field.value = value ?? '';
  wrapper.append(field);
  form.append(wrapper);
  return field;
}

function button(doc, label, handler) {
  const result = element(doc, 'button', label);
  result.type = 'button';
  result.addEventListener('click', handler);
  return result;
}

// This workspace only exposes the authenticated host's own listings.
export function initHostWorkspace(container, apiBase, getSession, doc = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container) return null;
  const section = element(doc, 'section', undefined, 'host-workspace');
  const heading = element(doc, 'h2', 'Mis alojamientos');
  const intro = element(doc, 'p', 'Crea tus anuncios y administra sus fechas disponibles.');
  const feedback = element(doc, 'p', '', 'feedback');
  feedback.setAttribute('role', 'status');
  const toolbar = element(doc, 'div', undefined, 'workspace-toolbar');
  const searchLabel = element(doc, 'label', 'Buscar en esta página');
  const search = element(doc, 'input'); search.type = 'search'; search.name = 'hostQuery';
  search.placeholder = 'Título o ciudad'; searchLabel.append(search);
  const stateLabel = element(doc, 'label', 'Estado en esta página');
  const state = element(doc, 'select'); state.name = 'hostStatus';
  const states = { DRAFT: 'Borrador', ACTIVE: 'Activo', PAUSED: 'Pausado', BLOCKED: 'Bloqueado' };
  for (const [value, text] of [['', 'Todos'], ...Object.entries(states)]) {
    const option = element(doc, 'option', text); option.value = value; state.append(option);
  }
  state.value = ''; stateLabel.append(state);
  const count = element(doc, 'p', '', 'workspace-count'); count.setAttribute('role', 'status');
  toolbar.append(searchLabel, stateLabel, button(doc, 'Crear alojamiento', () => showEditor()), count);
  toolbar.hidden = true;
  const editor = element(doc, 'div');
  const items = element(doc, 'div', undefined, 'host-list');
  const paging = element(doc, 'div', undefined, 'host-actions workspace-pagination');
  section.append(heading, intro, toolbar, feedback, editor, items, paging);
  container.append(section);
  let revision = 0;
  let page = 0;
  let draw = () => {};
  search.addEventListener('input', () => draw());
  state.addEventListener('change', () => draw());
  const token = () => getSession()?.user?.role === 'HOST' ? getSession()?.accessToken : null;
  const current = (captured, request) => captured && captured === token() && request === revision;
  function clear() {
    revision += 1;
    page = 0;
    editor.replaceChildren();
    items.replaceChildren();
    paging.replaceChildren();
    feedback.textContent = '';
    count.textContent = ''; toolbar.hidden = true;
    search.value = ''; state.value = ''; draw = () => {};
  }
  async function api(path, captured, options = {}) {
    const response = await fetchImpl(new URL(path, apiBase), {
      ...options, headers: { Authorization: `Bearer ${captured}`, ...(options.headers || {}) },
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'No fue posible completar la operación.');
    return result;
  }
  async function send(path, captured, method, data) {
    return api(path, captured, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
  }
  function showEditor(listing = null) {
    editor.replaceChildren();
    const form = element(doc, 'form', undefined, 'host-form');
    form.append(element(doc, 'h3', listing ? 'Editar alojamiento' : 'Crear alojamiento'));
    const title = control(doc, form, 'Título', 'title', 'text', listing?.title);
    const description = control(doc, form, 'Descripción', 'description', 'textarea', listing?.description);
    const city = control(doc, form, 'Ciudad y departamento', 'city', 'text', listing?.city);
    const location = control(doc, form, 'Ubicación o sector', 'location', 'text', listing?.location);
    const typeLabel = element(doc, 'label', 'Tipo de alojamiento');
    const type = element(doc, 'select'); type.name = 'type';
    for (const [value, text] of [['APARTMENT', 'Apartamento'], ['HOUSE', 'Casa'], ['ROOM', 'Habitación'], ['CABIN', 'Cabaña']]) {
      const option = element(doc, 'option', text); option.value = value; type.append(option);
    }
    if (listing?.type && !['APARTMENT', 'HOUSE', 'ROOM', 'CABIN'].includes(listing.type)) {
      const option = element(doc, 'option', listing.type); option.value = listing.type; type.append(option);
    }
    type.value = listing?.type || 'APARTMENT'; typeLabel.append(type); form.append(typeLabel);
    const capacity = control(doc, form, 'Capacidad de huéspedes', 'capacity', 'number', listing?.capacity || 1);
    capacity.min = '1'; capacity.step = '1';
    const price = control(doc, form, 'Precio por noche (COP)', 'pricePerNight', 'number', listing?.pricePerNight || '');
    price.min = '0.01'; price.step = '0.01';
    for (const input of [title, description, city, location, capacity, price]) input.required = true;
    const amenityGroup = element(doc, 'fieldset'); amenityGroup.append(element(doc, 'legend', 'Servicios'));
    const amenities = [];
    for (const [value, label] of [['WIFI', 'Wi-Fi'], ['PARKING', 'Parqueadero'], ['POOL', 'Piscina'], ['AIR_CONDITIONING', 'Aire acondicionado']]) {
      const wrapper = element(doc, 'label', label);
      const box = element(doc, 'input'); box.type = 'checkbox'; box.value = value;
      box.checked = (listing?.amenityIds || []).includes(value); wrapper.append(box); amenityGroup.append(wrapper); amenities.push(box);
    }
    const otherAmenities = (listing?.amenityIds || []).filter((id) => !amenities.some((box) => box.value === id));
    form.append(amenityGroup);
    const rules = control(doc, form, 'Reglas de la casa', 'rules', 'textarea', listing?.rules);
    const images = control(doc, form, 'Enlaces de imágenes (uno por línea)', 'images', 'textarea', (listing?.imageReferences || []).join('\n'));
    const extraRows = element(doc, 'div');
    form.append(element(doc, 'h4', 'Servicios adicionales opcionales'), extraRows);
    const extras = [];
    function addExtra(extra = {}) {
      const row = element(doc, 'div', undefined, 'host-extra');
      const name = control(doc, row, 'Nombre del servicio', 'extraName', 'text', extra.name);
      const amount = control(doc, row, 'Precio (COP)', 'extraPrice', 'number', extra.unitPrice);
      amount.min = '0.01'; amount.step = '0.01';
      const state = { row, name, amount, id: extra.id, removed: false }; extras.push(state);
      row.append(button(doc, 'Quitar servicio', () => { state.removed = true; row.hidden = true; }));
      extraRows.append(row);
    }
    (listing?.extraOptions || []).forEach(addExtra);
    form.append(button(doc, 'Añadir servicio', () => addExtra()));
    const policy = listing?.cancellationPolicy || { freeRefundDays: 7, partialRefundDays: 2, partialRefundPercentage: 50 };
    const freeDays = control(doc, form, 'Días para reembolso completo', 'freeDays', 'number', policy.freeRefundDays);
    const partialDays = control(doc, form, 'Días para reembolso parcial', 'partialDays', 'number', policy.partialRefundDays);
    const percentage = control(doc, form, 'Reembolso parcial (%)', 'percentage', 'number', policy.partialRefundPercentage);
    for (const input of [freeDays, partialDays, percentage]) { input.min = '0'; input.required = true; }
    percentage.max = '100'; percentage.step = '0.01';
    const message = element(doc, 'p', '', 'feedback'); message.setAttribute('role', 'status');
    const submit = element(doc, 'button', listing ? 'Guardar cambios' : 'Crear alojamiento'); submit.type = 'submit';
    form.append(submit, button(doc, 'Cancelar', () => editor.replaceChildren()), message);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (typeof form.reportValidity === 'function' && !form.reportValidity()) return;
      const captured = token(); const request = revision;
      if (!captured) return;
      const extraOptions = extras.filter((row) => !row.removed).map((row) => ({
        ...(row.id ? { id: row.id } : {}), name: row.name.value.trim(), unitPrice: Number(row.amount.value),
      }));
      if (extraOptions.some((extra) => !extra.name || !(extra.unitPrice > 0))) {
        message.textContent = 'Cada servicio adicional necesita nombre y precio positivo.'; return;
      }
      if (Number(freeDays.value) < Number(partialDays.value)) {
        message.textContent = 'Los días de reembolso completo deben ser mayores o iguales a los del parcial.'; return;
      }
      const body = { title: title.value, description: description.value, city: city.value, location: location.value,
        type: type.value, capacity: Number(capacity.value), pricePerNight: Number(price.value),
        amenityIds: [...otherAmenities, ...amenities.filter((box) => box.checked).map((box) => box.value)], rules: rules.value,
        imageReferences: images.value.split(/\r?\n/).map((value) => value.trim()).filter(Boolean),
        extraOptions, cancellationPolicy: { freeRefundDays: Number(freeDays.value), partialRefundDays: Number(partialDays.value),
          partialRefundPercentage: Number(percentage.value) } };
      submit.disabled = true;
      try {
        await send(listing ? `/api/listings/${listing.id}` : '/api/listings', captured, listing ? 'PATCH' : 'POST', body);
        if (!current(captured, request)) return;
        editor.replaceChildren();
        await refresh(listing ? 'Alojamiento actualizado.' : 'Alojamiento creado como borrador.');
      } catch (error) {
        if (current(captured, request)) { message.textContent = error.message; submit.disabled = false; }
      }
    });
    editor.append(form);
    editor.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  }
  async function stateChange(listing, status, captured, request) {
    try {
      await send(`/api/listings/${listing.id}/state`, captured, 'PATCH', { status });
      if (current(captured, request)) await refresh(status === 'ACTIVE' ? 'Publicación activada.' : 'Publicación pausada.');
    } catch (error) { if (current(captured, request)) feedback.textContent = error.message; }
  }
  async function availabilityEditor(listing, target, captured, request) {
    target.replaceChildren(element(doc, 'p', 'Cargando disponibilidad…'));
    try {
      const periods = await api(`/api/listings/${listing.id}/availability`, captured);
      if (!current(captured, request)) return;
      target.replaceChildren(element(doc, 'h4', 'Disponibilidad'));
      target.append(element(doc, 'p', 'Las fechas de salida no se incluyen. Las reservas confirmadas se conservan.'));
      const editable = periods.filter((period) => period.type !== 'RESERVED');
      for (const period of periods) {
        const row = element(doc, 'div', undefined, 'host-period');
        row.append(element(doc, 'span', `${period.startDate} → ${period.endDate}: ${period.type}`));
        if (period.type !== 'RESERVED') row.append(button(doc, 'Quitar período', async () => {
          try {
            await send(`/api/listings/${listing.id}/availability`, captured, 'PUT',
              { periods: editable.filter((item) => item.id !== period.id).map(({ startDate, endDate, type }) => ({ startDate, endDate, type })) });
            if (current(captured, request)) await availabilityEditor(listing, target, captured, request);
          } catch (error) { if (current(captured, request)) feedback.textContent = error.message; }
        }));
        target.append(row);
      }
      const form = element(doc, 'form', undefined, 'host-form');
      const start = control(doc, form, 'Desde', 'start', 'date');
      const end = control(doc, form, 'Hasta', 'end', 'date');
      start.required = true; end.required = true;
      const typeLabel = element(doc, 'label', 'Tipo de período');
      const type = element(doc, 'select');
      for (const [value, text] of [['AVAILABLE', 'Disponible'], ['BLOCKED', 'Bloqueado']]) {
        const option = element(doc, 'option', text); option.value = value; type.append(option);
      }
      type.value = 'AVAILABLE'; typeLabel.append(type); form.append(typeLabel);
      const add = element(doc, 'button', 'Añadir período'); add.type = 'submit'; form.append(add);
      const message = element(doc, 'p', '', 'feedback'); message.setAttribute('role', 'status'); form.append(message);
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!start.value || !end.value || end.value <= start.value) { message.textContent = 'Indica un intervalo válido.'; return; }
        add.disabled = true;
        try {
          const rest = editable.map(({ startDate, endDate, type: itemType }) => ({ startDate, endDate, type: itemType }));
          await send(`/api/listings/${listing.id}/availability`, captured, 'PUT',
            { periods: [...rest, { startDate: start.value, endDate: end.value, type: type.value }] });
          if (current(captured, request)) await availabilityEditor(listing, target, captured, request);
        } catch (error) { if (current(captured, request)) { message.textContent = error.message; add.disabled = false; } }
      });
      target.append(form);
    } catch (error) { if (current(captured, request)) target.replaceChildren(element(doc, 'p', error.message)); }
  }
  function renderListing(listing, captured, request) {
    const card = element(doc, 'article', undefined, 'host-card');
    card.append(element(doc, 'h3', listing.title), element(doc, 'p', listing.city),
      element(doc, 'span', states[listing.status] || listing.status, `status-badge status-${String(listing.status).toLowerCase()}`),
      element(doc, 'p', `${Number(listing.pricePerNight || 0).toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })} por noche · ${listing.capacity} huéspedes`));
    const actions = element(doc, 'div', undefined, 'host-actions');
    if (listing.status !== 'BLOCKED') {
      actions.append(button(doc, 'Editar', () => showEditor(listing)));
      if (['DRAFT', 'PAUSED'].includes(listing.status)) actions.append(button(doc, 'Activar', () => stateChange(listing, 'ACTIVE', captured, request)));
      if (listing.status === 'ACTIVE') actions.append(button(doc, 'Pausar', () => stateChange(listing, 'PAUSED', captured, request)));
      actions.append(button(doc, 'Administrar disponibilidad', () => {
        editor.replaceChildren();
        const availability = element(doc, 'div');
        editor.append(button(doc, 'Cerrar disponibilidad', () => editor.replaceChildren()), availability);
        editor.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
        return availabilityEditor(listing, availability, captured, request);
      }));
      card.append(actions);
    } else {
      card.append(element(doc, 'p', 'Esta publicación fue bloqueada por administración.'));
    }
    return card;
  }
  async function refresh(notice = '') {
    const captured = token(); const request = ++revision;
    items.replaceChildren(); paging.replaceChildren(); editor.replaceChildren();
    draw = () => {}; count.textContent = ''; toolbar.hidden = !captured;
    if (!captured) { feedback.textContent = ''; return; }
    feedback.textContent = 'Cargando tus alojamientos…';
    try {
      const result = await api(`/api/host/listings?page=${page}&size=20`, captured);
      if (!current(captured, request)) return;
      if (!Array.isArray(result.content)) throw new Error('No fue posible cargar tus alojamientos.');
      feedback.textContent = notice || (result.content?.length ? '' : 'Aún no tienes alojamientos.');
      draw = () => {
        if (!current(captured, request)) return;
        const normalize = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
        const term = normalize(search.value).trim();
        const selected = result.content.filter((listing) => (!state.value || listing.status === state.value)
          && normalize(`${listing.title} ${listing.city}`).includes(term));
        items.replaceChildren();
        for (const listing of selected) items.append(renderListing(listing, captured, request));
        if (!selected.length) items.append(element(doc, 'p', result.content.length
          ? 'No hay alojamientos que coincidan en esta página.' : 'Crea tu primer alojamiento para empezar.', 'empty-state'));
        count.textContent = `${selected.length} de ${result.content.length} en esta página · ${result.totalElements ?? result.content.length} alojamientos en total`;
      };
      draw();
      if (page > 0) paging.append(button(doc, 'Anterior', () => { page -= 1; void refresh(); }));
      paging.append(element(doc, 'span', `Página ${page + 1} de ${Math.max(1, Number(result.totalPages) || 1)}`));
      if (page + 1 < result.totalPages) paging.append(button(doc, 'Siguiente', () => { page += 1; void refresh(); }));
    } catch (error) { if (current(captured, request)) feedback.textContent = error.message; }
  }
  return { section, refresh, clear };
}
