function el(doc, tag, text, className = '') {
  const node = doc.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function button(doc, label, action) {
  const node = el(doc, 'button', label);
  node.type = 'button';
  node.addEventListener('click', action);
  return node;
}

function status(doc) {
  const node = el(doc, 'p', '', 'admin-feedback');
  node.setAttribute('role', 'status');
  node.setAttribute('aria-live', 'polite');
  return node;
}

function isValid(form) {
  return typeof form.reportValidity !== 'function' || form.reportValidity();
}

export function initAdministration(container, apiBase, getToken, doc = globalThis.document,
  fetchImpl = globalThis.fetch, download = defaultDownload) {
  if (!container) return null;
  const section = el(doc, 'section', undefined, 'admin-workspace');
  section.append(el(doc, 'h2', 'Administración y reportes'));
  const introduction = el(doc, 'p', 'Consulta indicadores, gestiona cuentas y publicaciones, y revisa la auditoría.');
  const feedback = status(doc);
  const filters = el(doc, 'form', undefined, 'admin-filters');
  const from = el(doc, 'input'); from.type = 'date'; from.name = 'from';
  const to = el(doc, 'input'); to.type = 'date'; to.name = 'to';
  const fromLabel = el(doc, 'label', 'Desde'); fromLabel.append(from);
  const toLabel = el(doc, 'label', 'Hasta'); toLabel.append(to);
  const apply = el(doc, 'button', 'Consultar indicadores'); apply.type = 'submit';
  filters.append(fromLabel, toLabel, apply);
  const dashboard = el(doc, 'div', undefined, 'admin-dashboard');
  const reportActions = el(doc, 'div', undefined, 'admin-actions');
  const reportFeedback = status(doc);
  const users = group('Cuentas');
  const listings = group('Publicaciones');
  const reviewQueue = group('Reservas para revisión manual');
  const audit = group('Auditoría');
  section.append(introduction, filters, feedback, dashboard, reportActions, reportFeedback,
    users.section, listings.section, reviewQueue.section, audit.section);
  container.append(section);
  let revision = 0;
  let dashboardRevision = 0;
  let appliedDates = null;
  let userPage = 0; let listingPage = 0; let auditPage = 0;

  function group(title) {
    const panel = el(doc, 'section', undefined, 'admin-group');
    const heading = el(doc, 'h3', title);
    const info = status(doc);
    const items = el(doc, 'div', undefined, 'admin-items');
    const paging = el(doc, 'div', undefined, 'admin-actions');
    panel.append(heading, info, items, paging);
    return { section: panel, info, items, paging, request: 0 };
  }

  function url(path, query = {}) {
    const result = new URL(path, apiBase);
    for (const [key, value] of Object.entries(query)) if (value !== '' && value !== undefined) result.searchParams.set(key, value);
    return result;
  }

  function dates() { return { from: from.value || '', to: to.value || '' }; }
  function current(token, request) { return request === revision && token && getToken() === token; }
  async function jsonRequest(path, token, query = {}, options = {}) {
    const response = await fetchImpl(url(path, query), {
      ...options, headers: { Authorization: `Bearer ${token}`, ...options.headers },
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.message || 'No fue posible completar la consulta.');
    return body;
  }

  async function loadDashboard(token, request) {
    const currentDashboard = ++dashboardRevision;
    const selectedDates = dates();
    feedback.textContent = 'Cargando indicadores…';
    try {
      const data = await jsonRequest('/api/admin/dashboard', token, selectedDates);
      if (!current(token, request) || currentDashboard !== dashboardRevision) return;
      appliedDates = selectedDates;
      dashboard.replaceChildren();
      const facts = [
        ['Usuarios', data.users], ['Publicaciones activas', data.activeListings],
        ['Ingresos estimados', `${Number(data.estimatedRevenue || 0).toLocaleString('es-CO')} ${data.currency || 'COP'}`],
      ];
      for (const [label, value] of facts) {
        const card = el(doc, 'article', undefined, 'admin-metric');
        card.append(el(doc, 'h4', label), el(doc, 'strong', String(value ?? 0)));
        dashboard.append(card);
      }
      const bookings = el(doc, 'article', undefined, 'admin-metric');
      bookings.append(el(doc, 'h4', 'Reservas por estado'));
      for (const [name, count] of Object.entries(data.bookingsByStatus || {})) bookings.append(el(doc, 'p', `${name}: ${count}`));
      const cities = el(doc, 'article', undefined, 'admin-metric');
      cities.append(el(doc, 'h4', 'Ciudades con mayor demanda'));
      for (const city of data.topCities || []) cities.append(el(doc, 'p', `${city.city}: ${city.bookings} reserva(s)`));
      if (!(data.topCities || []).length) cities.append(el(doc, 'p', 'Sin reservas en el periodo.'));
      dashboard.append(bookings, cities);
      const definitions = el(doc, 'details', undefined, 'admin-definitions');
      definitions.append(el(doc, 'summary', 'Cómo se calculan los indicadores'));
      for (const [key, definition] of Object.entries(data.definitions || {})) definitions.append(el(doc, 'p', `${key}: ${definition}`));
      dashboard.append(definitions);
      feedback.textContent = `Fecha de corte: ${data.generatedAt || 'actual'} · Zona horaria: ${data.timezone || 'America/Bogota'}. Los ingresos son estimados.`;
    } catch (error) {
      if (!current(token, request) || currentDashboard !== dashboardRevision) return;
      appliedDates = null; dashboard.replaceChildren(); feedback.textContent = error.message || 'No fue posible consultar los indicadores.';
    }
  }

  async function loadPage(group, path, page, token, request, renderItem, setPage) {
    const currentPageRequest = ++group.request;
    group.info.textContent = 'Cargando…';
    try {
      const data = await jsonRequest(path, token, { page, size: 20 });
      if (!current(token, request) || currentPageRequest !== group.request) return;
      if (!Array.isArray(data.content)) throw new Error('La respuesta no tiene el formato esperado.');
      group.items.replaceChildren(); group.paging.replaceChildren();
      group.info.textContent = data.content.length ? `${data.totalElements ?? data.content.length} registro(s).` : 'No hay registros.';
      for (const item of data.content) group.items.append(renderItem(item, token, request));
      if (page > 0) group.paging.append(button(doc, 'Anterior', () => { setPage(page - 1); void loadPage(group, path, page - 1, token, request, renderItem, setPage); }));
      if (page + 1 < Number(data.totalPages || 0)) group.paging.append(button(doc, 'Siguiente', () => { setPage(page + 1); void loadPage(group, path, page + 1, token, request, renderItem, setPage); }));
    } catch (error) {
      if (!current(token, request) || currentPageRequest !== group.request) return;
      group.items.replaceChildren(); group.paging.replaceChildren(); group.info.textContent = error.message || 'No fue posible cargar los registros.';
    }
  }

  function moderationForm(label, action, token, request, onSuccess) {
    const form = el(doc, 'form', undefined, 'admin-moderation-form');
    const reason = el(doc, 'textarea'); reason.required = true; reason.maxLength = 1000; reason.rows = 2;
    const reasonLabel = el(doc, 'label', 'Motivo'); reasonLabel.append(reason);
    const submit = el(doc, 'button', label); submit.type = 'submit';
    const info = status(doc);
    form.append(reasonLabel, submit, info);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (!isValid(form)) return;
      const value = (reason.value || '').trim();
      if (!value) { info.textContent = 'Indica un motivo.'; return; }
      if (!current(token, request)) return;
      submit.disabled = true; info.textContent = 'Guardando…';
      try {
        await action(value);
        if (!current(token, request)) return;
        await onSuccess();
      } catch (error) {
        if (!current(token, request)) return;
        info.textContent = error.message || 'No fue posible completar la acción.'; submit.disabled = false;
      }
    });
    return form;
  }

  function userCard(user, token, request) {
    const card = el(doc, 'article', undefined, 'admin-card');
    card.append(el(doc, 'h4', user.name || 'Usuario'), el(doc, 'p', `${user.email || ''} · ${user.role || ''} · ${user.accountState || ''}`));
    if (user.accountState === 'ACTIVE') {
      card.append(moderationForm('Restringir cuenta', (reason) => jsonRequest(`/api/admin/users/${encodeURIComponent(user.id)}/restrict`, token, {}, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason }),
      }), token, request, async () => { await loadUsers(token, request); await loadAudit(token, request); }));
    }
    return card;
  }

  function listingCard(listing, token, request) {
    const card = el(doc, 'article', undefined, 'admin-card');
    card.append(el(doc, 'h4', listing.title || 'Publicación'), el(doc, 'p', `${listing.city || ''} · ${listing.status || ''}`));
    if (listing.status !== 'BLOCKED') {
      card.append(moderationForm('Bloquear publicación', (reason) => jsonRequest(`/api/listings/${encodeURIComponent(listing.id)}/state`, token, {}, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status: 'BLOCKED', reason }),
      }), token, request, async () => { await loadListings(token, request); await loadQueue(token, request); await loadAudit(token, request); }));
    }
    return card;
  }

  function auditCard(event) {
    const card = el(doc, 'article', undefined, 'admin-card');
    card.append(el(doc, 'h4', `${event.eventType || 'Acción'} · ${event.result || ''}`));
    card.append(el(doc, 'p', `Fecha: ${event.occurredAt || 'No disponible'} · Actor: ${event.actorId || 'Sistema'}`));
    card.append(el(doc, 'p', `Entidad: ${event.entityType || ''} ${event.entityId || ''}`));
    if (event.reason) card.append(el(doc, 'p', `Motivo: ${event.reason}`));
    return card;
  }

  const loadUsers = (token, request) => loadPage(users, '/api/admin/users', userPage, token, request, userCard, (page) => { userPage = page; });
  const loadListings = (token, request) => loadPage(listings, '/api/admin/listings', listingPage, token, request, listingCard, (page) => { listingPage = page; });
  const loadAudit = (token, request) => loadPage(audit, '/api/admin/audit', auditPage, token, request, auditCard, (page) => { auditPage = page; });
  async function loadQueue(token, request) {
    reviewQueue.info.textContent = 'Cargando…';
    try {
      const data = await jsonRequest('/api/admin/reservations/manual-review', token);
      if (!current(token, request)) return;
      if (!Array.isArray(data)) throw new Error('La respuesta no tiene el formato esperado.');
      reviewQueue.items.replaceChildren();
      reviewQueue.info.textContent = data.length ? `${data.length} reserva(s) para revisión manual.` : 'No hay reservas para revisión manual.';
      for (const item of data) {
        const card = el(doc, 'article', undefined, 'admin-card');
        card.append(el(doc, 'h4', item.listingTitle || 'Reserva'), el(doc, 'p', `${item.checkIn} a ${item.checkOut} · ${item.status}`));
        card.append(el(doc, 'p', `Reserva: ${item.id}`));
        card.append(moderationForm('Marcar como revisada', (reason) => jsonRequest(`/api/admin/reservations/${encodeURIComponent(item.id)}/resolve-review`, token, {}, {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason }),
        }), token, request, async () => { await loadQueue(token, request); await loadAudit(token, request); }));
        reviewQueue.items.append(card);
      }
    } catch (error) {
      if (!current(token, request)) return;
      reviewQueue.items.replaceChildren(); reviewQueue.info.textContent = error.message || 'No fue posible consultar las reservas.';
    }
  }

  async function exportReport(format) {
    const token = getToken(); const request = revision;
    if (!token) return;
    if (from.value && to.value && from.value > to.value) {
      reportFeedback.textContent = 'La fecha inicial debe ser anterior o igual a la final.';
      return;
    }
    const selectedDates = dates();
    if (!appliedDates || selectedDates.from !== appliedDates.from || selectedDates.to !== appliedDates.to) {
      reportFeedback.textContent = 'Consulta los indicadores con las fechas seleccionadas antes de exportar.';
      return;
    }
    reportFeedback.textContent = 'Preparando reporte…';
    try {
      const response = await fetchImpl(url('/api/admin/reports', { ...appliedDates, format }), { headers: { Authorization: `Bearer ${token}` } });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'No fue posible exportar el reporte.');
      }
      const blob = await response.blob();
      if (!current(token, request)) return;
      download(blob, `reporte-marketplace.${format.toLowerCase()}`, doc);
      reportFeedback.textContent = 'Reporte generado.';
      await loadAudit(token, request);
    } catch (error) {
      if (!current(token, request)) return;
      reportFeedback.textContent = error.message || 'No fue posible exportar el reporte.';
    }
  }
  reportActions.append(button(doc, 'Descargar PDF', () => { void exportReport('PDF'); }),
    button(doc, 'Descargar Excel', () => { void exportReport('XLSX'); }));
  filters.addEventListener('submit', (event) => {
    event.preventDefault();
    const token = getToken(); const request = revision;
    if (!token) return;
    if (from.value && to.value && from.value > to.value) { feedback.textContent = 'La fecha inicial debe ser anterior o igual a la final.'; return; }
    void loadDashboard(token, request);
  });

  async function refresh() {
    const token = getToken();
    const request = ++revision;
    if (!token) { clear(); return; }
    section.hidden = false;
    await Promise.all([loadDashboard(token, request), loadUsers(token, request), loadListings(token, request),
      loadQueue(token, request), loadAudit(token, request)]);
  }
  function clear() {
    revision += 1; dashboardRevision += 1; section.hidden = true;
    appliedDates = null;
    dashboard.replaceChildren(); feedback.textContent = ''; reportFeedback.textContent = '';
    for (const group of [users, listings, reviewQueue, audit]) {
      group.items.replaceChildren(); group.paging.replaceChildren(); group.info.textContent = '';
    }
    userPage = 0; listingPage = 0; auditPage = 0; from.value = ''; to.value = '';
  }
  clear();
  return { section, refresh, clear };
}

function defaultDownload(blob, filename, doc) {
  const href = URL.createObjectURL(blob);
  const anchor = doc.createElement('a'); anchor.href = href; anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(href);
}
