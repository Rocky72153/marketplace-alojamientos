import { buildSearchDetailUrl, readSearchFilters, searchListings, validateSearch, writeSearchLocation } from './search.js';

const TYPE_NAMES = { APARTMENT: 'Apartamento', HOUSE: 'Casa', ROOM: 'Habitación', CABIN: 'Cabaña' };

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
  let revision = 0;
  let searched = false;
  let lastFilters;
  let items = [];
  let page = 0;

  function restore(filters) {
    for (const field of form.querySelectorAll('input, select')) {
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

  function renderPage(nextPage = 0) {
    const pages = Math.ceil(items.length / pageSize);
    page = Math.min(Math.max(0, nextPage), Math.max(0, pages - 1));
    results.replaceChildren();
    const offset = page * pageSize;
    for (const listing of items.slice(offset, offset + pageSize)) {
      const card = element(doc, 'article', undefined, 'listing-card');
      const location = `${listing.city} · ${TYPE_NAMES[listing.type] || listing.type} · Hasta ${listing.capacity} huéspedes`;
      const price = `$${Number(listing.pricePerNight).toLocaleString('es-CO')} por noche`;
      const detail = element(doc, 'a', 'Ver alojamiento', 'listing-detail-link');
      detail.href = buildSearchDetailUrl(listing.id, lastFilters, page);
      card.append(element(doc, 'h3', listing.title), element(doc, 'p', location),
        element(doc, 'p', price, 'listing-price'), detail);
      results.append(card);
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
    feedback.textContent = total ? `${total} alojamiento${total === 1 ? '' : 's'} encontrado${total === 1 ? '' : 's'}.`
      + (pages > 1 ? ` Mostrando ${offset + 1}–${Math.min(offset + pageSize, total)}.` : '')
      : 'No encontramos alojamientos con esos criterios.';
  }

  async function search(filters = readForm(), requestedPage = 0, updateLocation = true) {
    const request = ++revision;
    const error = validateSearch(filters);
    searched = true;
    if (error) { feedback.textContent = error; results.replaceChildren(); return; }
    lastFilters = { ...filters, amenityIds: [...(filters.amenityIds || [])] };
    // Record the user's search now; a late response must not change a newer view.
    if (updateLocation) writeSearchLocation(win, lastFilters, requestedPage);
    feedback.textContent = 'Buscando alojamientos…';
    results.replaceChildren();
    try {
      // The public API returns every matching item; paginate that response locally.
      const response = await searchListings(filters, apiBase, fetchImpl);
      if (request !== revision) return;
      items = Array.isArray(response.items) ? response.items : [];
      renderPage(requestedPage);
    } catch (error_) {
      if (request !== revision) return;
      feedback.textContent = error_.message || 'No fue posible consultar los alojamientos.';
      results.replaceChildren();
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

  const initial = readSearchFilters(query);
  if (query.has('city')) restore(initial);
  function resume() {
    if (!searched && query.has('city')) void search(initial, Number(query.get('searchPage')) || 0, false);
  }
  if (!query.has('listing') && !query.has('stripe_checkout')) resume();
  return { search, resume };
}
