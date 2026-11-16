import { buildSearchDetailUrl, readSearchFilters, searchListings, validateSearch, writeSearchLocation } from './search.js';
import { renderListingCard } from './listing-card.js';

function todayInBogota() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}-${values.month}-${values.day}`;
}

function element(doc, tag, text, className = '') {
  const node = doc.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

export function initSearchResults(doc, apiBase, options = {}) {
  const form = doc.querySelector('#search-form');
  const feedback = doc.querySelector('#search-feedback');
  const results = doc.querySelector('#search-results');
  if (!form || !feedback || !results) return null;
  const win = options.windowRef ?? globalThis.window;
  const fetchImpl = options.fetchImpl ?? globalThis.fetch;
  const FormDataImpl = options.FormDataImpl ?? doc.defaultView?.FormData ?? globalThis.FormData;
  const query = options.query instanceof URLSearchParams ? options.query
    : new URLSearchParams(options.query ?? win?.location?.search ?? '');
  const pageSize = Math.max(1, Math.floor(Number(options.pageSize) || 12));
  const today = todayInBogota();
  const checkin = doc.querySelector('#checkin');
  const checkout = doc.querySelector('#checkout');
  const summary = doc.querySelector('#search-summary');
  const resultHeading = doc.querySelector('#search-results-heading');
  const filterCount = doc.querySelector('#active-filter-count');
  let revision = 0;
  let searched = false;
  let lastFilters;
  let items = [];
  let page = 0;

  function restore(filters) {
    for (const field of form.querySelectorAll?.('input, select') || []) {
      if (!field.name) continue;
      if (field.name === 'amenityIds') field.checked = filters.amenityIds.includes(field.value);
      else if (Object.hasOwn(filters, field.name)) field.value = filters[field.name];
    }
    if (checkout) checkout.min = checkin?.value || today;
  }

  function readForm() {
    const data = new FormDataImpl(form);
    return { ...Object.fromEntries(data.entries()), amenityIds: data.getAll('amenityIds') };
  }

  function updateFilterCount() {
    if (!filterCount) return;
    const filters = readForm();
    const count = ['type', 'minPrice', 'maxPrice'].filter((name) => filters[name]).length + filters.amenityIds.length;
    filterCount.textContent = String(count);
    filterCount.hidden = count === 0;
    filterCount.setAttribute('aria-label', `${count} filtros seleccionados`);
  }

  function describeSearch(filters) {
    if (!summary) return;
    const dateLabel = (value) => {
      const date = new Date(`${value}T12:00:00-05:00`);
      if (!Number.isFinite(date.getTime())) return value;
      return new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', timeZone: 'America/Bogota' }).format(date);
    };
    const guests = Number(filters.guests);
    summary.textContent = `${filters.city} · ${dateLabel(filters.checkin)} – ${dateLabel(filters.checkout)} · ${guests} huésped${guests === 1 ? '' : 'es'}`;
    if (resultHeading) resultHeading.hidden = false;
  }

  function setLoading(loading) {
    results.setAttribute?.('aria-busy', String(loading));
    feedback.setAttribute?.('data-status', loading ? 'loading' : 'idle');
  }

  function showEmpty(title, description, retry = false) {
    const empty = element(doc, 'div', undefined, 'empty-state');
    const button = element(doc, 'button', retry ? 'Reintentar búsqueda' : 'Cambiar criterios', 'secondary-button');
    button.type = 'button';
    button.addEventListener('click', () => {
      if (retry) void search(lastFilters);
      else editSearch();
    });
    empty.append(element(doc, 'h3', title), element(doc, 'p', description), button);
    results.append(empty);
  }

  function editSearch() {
    form.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
    doc.querySelector('#city')?.focus();
  }

  function renderPage(nextPage = 0) {
    const pages = Math.ceil(items.length / pageSize);
    page = Math.min(Math.max(0, nextPage), Math.max(0, pages - 1));
    results.replaceChildren();
    const offset = page * pageSize;
    for (const listing of items.slice(offset, offset + pageSize)) {
      results.append(renderListingCard(doc, listing, { href: buildSearchDetailUrl(listing.id, lastFilters, page) }));
    }
    if (pages > 1) {
      const paging = element(doc, 'nav', undefined, 'search-pagination host-actions');
      paging.setAttribute('aria-label', 'Páginas de resultados');
      const previous = element(doc, 'button', 'Anterior', 'secondary-button');
      previous.type = 'button'; previous.disabled = page === 0;
      const next = element(doc, 'button', 'Siguiente', 'secondary-button');
      next.type = 'button'; next.disabled = page + 1 >= pages;
      const changePage = (value) => {
        renderPage(value);
        writeSearchLocation(win, lastFilters, page);
        results.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      };
      previous.addEventListener('click', () => changePage(page - 1));
      next.addEventListener('click', () => changePage(page + 1));
      paging.append(previous, element(doc, 'span', `Página ${page + 1} de ${pages}`), next);
      results.append(paging);
    }
    const total = items.length;
    const suffix = total === 1 ? '' : 's';
    const range = pages > 1 ? ` Mostrando ${offset + 1}–${Math.min(offset + pageSize, total)}.` : '';
    feedback.textContent = `${total} alojamiento${suffix} encontrado${suffix}.${range}`;
    if (!total) {
      feedback.textContent = 'No encontramos alojamientos con esos criterios.';
      showEmpty('Tu próximo destino aún puede estar aquí', 'Prueba otras fechas, amplía el rango de precio o quita algunos filtros.');
    }
    feedback.setAttribute?.('data-status', total ? 'success' : 'idle');
  }

  async function search(filters = readForm(), requestedPage = 0, updateLocation = true) {
    const request = ++revision;
    const error = validateSearch(filters);
    searched = true;
    if (error) {
      setLoading(false);
      feedback.textContent = error;
      feedback.setAttribute?.('data-status', 'error');
      results.replaceChildren();
      return;
    }
    lastFilters = { ...filters, amenityIds: [...(filters.amenityIds || [])] };
    // Record the user's search now; a late response must not change a newer view.
    if (updateLocation) writeSearchLocation(win, lastFilters, requestedPage);
    describeSearch(filters);
    feedback.textContent = 'Buscando alojamientos…';
    results.replaceChildren();
    setLoading(true);
    for (let index = 0; index < 3; index += 1) {
      const skeleton = element(doc, 'div', undefined, 'listing-skeleton');
      skeleton.setAttribute?.('aria-hidden', 'true');
      results.append(skeleton);
    }
    try {
      // The public API returns every matching item; paginate that response locally.
      const response = await searchListings(filters, apiBase, fetchImpl);
      if (request !== revision) return;
      setLoading(false);
      items = Array.isArray(response.items) ? response.items : [];
      renderPage(requestedPage);
    } catch (error_) {
      if (request !== revision) return;
      setLoading(false);
      feedback.textContent = error_.message || 'No fue posible consultar los alojamientos.';
      feedback.setAttribute?.('data-status', 'error');
      results.replaceChildren();
      showEmpty('No pudimos completar la búsqueda', 'Tus criterios se conservaron. Puedes intentarlo de nuevo.', true);
    }
  }

  if (checkin) checkin.min = today;
  if (checkout) checkout.min = today;
  checkin?.addEventListener('change', () => {
    if (!checkout) return;
    checkout.min = checkin.value || today;
    if (checkout.value && checkout.value <= checkin.value) checkout.value = '';
  });
  form.addEventListener('submit', (event) => { event.preventDefault(); void search(); });
  form.addEventListener('change', updateFilterCount);
  doc.querySelector('#edit-search')?.addEventListener('click', editSearch);
  function clearFilters() {
    for (const field of form.querySelectorAll?.('[name="type"], [name="minPrice"], [name="maxPrice"], [name="amenityIds"]') || []) {
      if (field.type === 'checkbox') field.checked = false;
      else field.value = '';
    }
    updateFilterCount();
    if (searched) void search();
  }
  doc.querySelector('#clear-search-filters')?.addEventListener('click', clearFilters);

  const initial = readSearchFilters(query);
  if (query.has('city')) restore(initial);
  updateFilterCount();
  function resume() {
    if (!searched && query.has('city')) void search(initial, Number(query.get('searchPage')) || 0, false);
  }
  if (!query.has('listing') && !query.has('stripe_checkout')) resume();
  return { search, resume, clearFilters };
}
