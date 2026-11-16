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
  const users = group('Cuentas', 'users', ['Persona', 'Rol', 'Estado', 'Acciones'], 'accountState');
  const listings = group('Publicaciones', 'listings', ['Alojamiento', 'Ciudad', 'Estado', 'Acciones'], 'status');
  const reviewQueue = group('Reservas para revisión manual', 'review', ['Reserva', 'Estadía', 'Estado', 'Acciones'], 'status');
  const audit = group('Auditoría', 'audit', ['Fecha', 'Evento', 'Actor', 'Entidad', 'Resultado', 'Detalle'], 'result');
  const overview = el(doc, 'section', undefined, 'workspace-panel'); overview.id = 'admin-overview';
  overview.append(introduction, filters, feedback, dashboard, reportActions, reportFeedback);
  const tabs = el(doc, 'div', undefined, 'workspace-tabs'); tabs.setAttribute('role', 'tablist');
  tabs.setAttribute('aria-label', 'Secciones de administración');
  const panels = [['Resumen', overview], ['Cuentas', users.section], ['Publicaciones', listings.section],
    ['Revisión manual', reviewQueue.section], ['Auditoría', audit.section]];
  const tabButtons = panels.map(([label, panel]) => {
    const tab = button(doc, label, () => selectPanel(panel));
    tab.className = 'workspace-tab'; tab.setAttribute('role', 'tab');
    tab.setAttribute('aria-controls', panel.id); panel.setAttribute('role', 'tabpanel');
    return tab;
  });
  function selectPanel(selected) {
    panels.forEach(([, panel], index) => {
      panel.hidden = panel !== selected;
      tabButtons[index].setAttribute('aria-selected', String(panel === selected));
      tabButtons[index].setAttribute('tabindex', panel === selected ? '0' : '-1');
    });
  }
  tabButtons.forEach((tab, index) => tab.addEventListener('keydown', (event) => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? panels.length - 1
      : (index + (event.key === 'ArrowRight' ? 1 : -1) + panels.length) % panels.length;
    selectPanel(panels[next][1]); tabButtons[next].focus?.();
  }));
  tabs.append(...tabButtons);
  section.append(tabs, overview, users.section, listings.section, reviewQueue.section, audit.section);
  selectPanel(overview);
  container.append(section);
  let revision = 0;
  let dashboardRevision = 0;
  let appliedDates = null;
  let userPage = 0; let listingPage = 0; let auditPage = 0;

  function group(title, key, columns, stateKey) {
    const panel = el(doc, 'section', undefined, 'admin-group workspace-panel'); panel.id = `admin-${key}`;
    const heading = el(doc, 'h3', title);
    const info = status(doc);
    const toolbar = el(doc, 'div', undefined, 'workspace-toolbar');
    const label = el(doc, 'label', key === 'review' ? 'Buscar reserva' : 'Buscar en esta página');
    const search = el(doc, 'input'); search.type = 'search'; search.name = `admin-${key}-query`;
    label.append(search);
    const stateLabel = el(doc, 'label', key === 'audit' ? 'Resultado en esta página' : 'Estado en esta página');
    const state = el(doc, 'select'); state.name = `admin-${key}-state`; stateLabel.append(state);
    toolbar.append(label, stateLabel);
    const items = el(doc, 'div', undefined, 'table-scroll');
    const paging = el(doc, 'div', undefined, 'admin-actions workspace-pagination');
    panel.append(heading, toolbar, info, items, paging);
    const result = { section: panel, title, columns, stateKey, search, state, info, items, paging, request: 0, draw: () => {} };
    search.addEventListener('input', () => result.draw()); state.addEventListener('change', () => result.draw());
    return result;
  }

  const stateLabels = { ACTIVE: 'Activo', DRAFT: 'Borrador', PAUSED: 'Pausado', BLOCKED: 'Bloqueado',
    RESTRICTED: 'Restringida', DEACTIVATED: 'Desactivada', CONFIRMED: 'Confirmada', COMPLETED: 'Completada',
    CANCELLED: 'Cancelada', PENDING_PAYMENT: 'Pendiente de pago', SUCCESS: 'Correcto', FAILURE: 'Fallido' };
  const badge = (value) => el(doc, 'span', stateLabels[value] || value || 'No disponible', `status-badge status-${String(value || 'unknown').toLowerCase()}`);
  const cell = (...children) => { const node = el(doc, 'td'); node.append(...children); return node; };
  const textCell = (text) => cell(el(doc, 'span', text ?? '—'));
  function drawTable(group, data, token, request, renderItem, page = 0, total = data.length, pages = 1) {
    const selectedState = group.state.value;
    group.state.replaceChildren();
    const values = [...new Set([...data.map((item) => item[group.stateKey]), selectedState].filter(Boolean))]
      .sort((left, right) => String(stateLabels[left] || left).localeCompare(String(stateLabels[right] || right), 'es'));
    for (const value of ['', ...values]) {
      const option = el(doc, 'option', value ? stateLabels[value] || value : 'Todos'); option.value = value;
      group.state.append(option);
    }
    group.state.value = selectedState || '';
    group.draw = () => {
      if (!current(token, request)) return;
      const normalize = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
      const term = normalize(group.search.value).trim();
      const selected = data.filter((item) => (!group.state.value || item[group.stateKey] === group.state.value)
        && normalize(Object.values(item).join(' ')).includes(term));
      const table = el(doc, 'table', undefined, 'workspace-table');
      table.append(el(doc, 'caption', group.title));
      const head = el(doc, 'thead'); const headings = el(doc, 'tr');
      for (const name of group.columns) { const th = el(doc, 'th', name); th.setAttribute('scope', 'col'); headings.append(th); }
      head.append(headings); const body = el(doc, 'tbody');
      for (const item of selected) body.append(renderItem(item, token, request));
      if (!selected.length) {
        const row = el(doc, 'tr'); const empty = el(doc, 'td', data.length ? 'No hay coincidencias con estos filtros.' : 'No hay registros.', 'empty-state');
        empty.colSpan = group.columns.length; row.append(empty); body.append(row);
      }
      table.append(head, body); group.items.replaceChildren(table);
      group.info.textContent = `${selected.length} de ${data.length} en esta página · ${total} registros en total`;
      if (group !== reviewQueue) group.info.textContent += ` · Página ${page + 1} de ${Math.max(1, pages)}`;
    };
    group.draw();
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
      for (const [name, count] of Object.entries(data.bookingsByStatus || {})) bookings.append(el(doc, 'p', `${stateLabels[name] || name}: ${count}`));
      const cities = el(doc, 'article', undefined, 'admin-metric');
      cities.append(el(doc, 'h4', 'Ciudades con mayor demanda'));
      for (const city of data.topCities || []) cities.append(el(doc, 'p', `${city.city}: ${city.bookings} reserva(s)`));
      if (!(data.topCities || []).length) cities.append(el(doc, 'p', 'Sin reservas en el periodo.'));
      dashboard.append(bookings, cities);
      const definitions = el(doc, 'details', undefined, 'admin-definitions');
      definitions.append(el(doc, 'summary', 'Cómo se calculan los indicadores'));
      for (const [key, definition] of Object.entries(data.definitions || {})) definitions.append(el(doc, 'p', `${key}: ${definition}`));
      dashboard.append(definitions);
      const generatedAt = new Date(data.generatedAt);
      const cutoff = Number.isFinite(generatedAt.getTime())
        ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' }).format(generatedAt) : 'actual';
      feedback.textContent = `Fecha de corte: ${cutoff} · Horario de Colombia. Los ingresos son estimados.`;
    } catch (error) {
      if (!current(token, request) || currentDashboard !== dashboardRevision) return;
      appliedDates = null; dashboard.replaceChildren(); feedback.textContent = error.message || 'No fue posible consultar los indicadores.';
    }
  }

  async function loadPage(group, path, page, token, request, renderItem, setPage) {
    const currentPageRequest = ++group.request;
    group.draw = () => {};
    group.info.textContent = 'Cargando…';
    try {
      const data = await jsonRequest(path, token, { page, size: 20 });
      if (!current(token, request) || currentPageRequest !== group.request) return;
      if (!Array.isArray(data.content)) throw new Error('La respuesta no tiene el formato esperado.');
      group.items.replaceChildren(); group.paging.replaceChildren();
      drawTable(group, data.content, token, request, renderItem, page, data.totalElements ?? data.content.length, Number(data.totalPages || 1));
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

  function actionDisclosure(label, action, token, request, onSuccess) {
    const details = el(doc, 'details', undefined, 'action-disclosure');
    details.append(el(doc, 'summary', label), moderationForm(label, action, token, request, onSuccess));
    return details;
  }

  function userCard(user, token, request) {
    const card = el(doc, 'tr');
    const identity = cell(el(doc, 'h4', user.name || 'Usuario'), el(doc, 'p', user.email || ''));
    const actions = cell(); actions.className = 'row-actions';
    card.append(identity, textCell({ HOST: 'Anfitrión', GUEST: 'Huésped', ADMIN: 'Administrador' }[user.role] || user.role), cell(badge(user.accountState)), actions);
    if (user.accountState === 'ACTIVE') {
      actions.append(actionDisclosure('Restringir cuenta', (reason) => jsonRequest(`/api/admin/users/${encodeURIComponent(user.id)}/restrict`, token, {}, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason }),
      }), token, request, async () => { await loadUsers(token, request); await loadAudit(token, request); }));
    }
    return card;
  }

  function listingCard(listing, token, request) {
    const card = el(doc, 'tr'); const actions = cell(); actions.className = 'row-actions';
    card.append(cell(el(doc, 'h4', listing.title || 'Publicación')), textCell(listing.city), cell(badge(listing.status)), actions);
    if (listing.status !== 'BLOCKED') {
      actions.append(actionDisclosure('Bloquear publicación', (reason) => jsonRequest(`/api/listings/${encodeURIComponent(listing.id)}/state`, token, {}, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status: 'BLOCKED', reason }),
      }), token, request, async () => { await loadListings(token, request); await loadQueue(token, request); await loadAudit(token, request); }));
    }
    return card;
  }

  function auditCard(event) {
    const card = el(doc, 'tr'); const detail = cell();
    if (event.correlationId) detail.append(el(doc, 'p', `Referencia de solicitud: ${event.correlationId}`));
    if (event.reason) detail.append(el(doc, 'p', `Motivo: ${event.reason}`));
    card.append(textCell(event.occurredAt), textCell(event.eventType || 'Acción'), textCell(event.actorId || 'Sistema'),
      textCell(`${event.entityType || ''} ${event.entityId || ''}`), cell(badge(event.result)), detail);
    return card;
  }

  const loadUsers = (token, request) => loadPage(users, '/api/admin/users', userPage, token, request, userCard, (page) => { userPage = page; });
  const loadListings = (token, request) => loadPage(listings, '/api/admin/listings', listingPage, token, request, listingCard, (page) => { listingPage = page; });
  const loadAudit = (token, request) => loadPage(audit, '/api/admin/audit', auditPage, token, request, auditCard, (page) => { auditPage = page; });
  async function loadQueue(token, request) {
    const queueRequest = ++reviewQueue.request;
    reviewQueue.draw = () => {};
    reviewQueue.info.textContent = 'Cargando…';
    try {
      const data = await jsonRequest('/api/admin/reservations/manual-review', token);
      if (!current(token, request) || queueRequest !== reviewQueue.request) return;
      if (!Array.isArray(data)) throw new Error('La respuesta no tiene el formato esperado.');
      reviewQueue.items.replaceChildren();
      drawTable(reviewQueue, data, token, request, (item) => {
        const card = el(doc, 'tr'); const actions = cell(); actions.className = 'row-actions';
        card.append(cell(el(doc, 'h4', item.listingTitle || 'Reserva'), el(doc, 'p', `Reserva: ${item.id}`)),
          textCell(`${item.checkIn} a ${item.checkOut}`), cell(badge(item.status)), actions);
        actions.append(actionDisclosure('Marcar como revisada', (reason) => jsonRequest(`/api/admin/reservations/${encodeURIComponent(item.id)}/resolve-review`, token, {}, {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason }),
        }), token, request, async () => { await loadQueue(token, request); await loadAudit(token, request); }));
        return card;
      });
    } catch (error) {
      if (!current(token, request) || queueRequest !== reviewQueue.request) return;
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
      group.search.value = ''; group.state.replaceChildren(); group.state.value = ''; group.draw = () => {};
    }
    selectPanel(overview);
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
