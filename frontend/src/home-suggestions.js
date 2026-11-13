import { buildListingDetailUrl } from './detail.js';

const activeRequests = new WeakMap();
const TYPE_NAMES = {
  APARTMENT: 'Apartamento',
  HOUSE: 'Casa',
  ROOM: 'Habitación',
  CABIN: 'Cabaña',
};

function element(doc, tag, className, text) {
  const node = doc.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

/**
 * Render independent public suggestions; it never alters search results or
 * reads a session, and ignores responses from an older refresh.
 */
export async function renderHomeSuggestions(container, apiBase, doc = globalThis.document,
  fetchImpl = globalThis.fetch) {
  if (!container) return;
  const requestId = (activeRequests.get(container) || 0) + 1;
  activeRequests.set(container, requestId);
  const heading = element(doc, 'h2', '', 'Descubre alojamientos');
  const feedback = element(doc, 'p', 'feedback', 'Cargando sugerencias…');
  feedback.setAttribute('role', 'status');
  feedback.setAttribute('aria-live', 'polite');
  const items = element(doc, 'div', 'listing-results');
  container.replaceChildren(heading, feedback, items);

  try {
    const response = await fetchImpl(new URL('/api/listings/suggestions', apiBase), {
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) throw new Error('No fue posible cargar las sugerencias.');
    const body = await response.json();
    if (activeRequests.get(container) !== requestId) return;
    if (!Array.isArray(body.items)) throw new Error('No fue posible cargar las sugerencias.');

    for (const listing of body.items.slice(0, 8)) {
      if (!listing.id) continue;
      const card = element(doc, 'article', 'listing-card');
      const title = element(doc, 'h3', '', listing.title || 'Alojamiento');
      const type = TYPE_NAMES[listing.type] || 'Alojamiento';
      const location = element(doc, 'p', '', `${listing.city || ''} · ${type} · Hasta ${listing.capacity} huéspedes`);
      const price = element(doc, 'p', 'listing-price',
        `$${Number(listing.pricePerNight).toLocaleString('es-CO')} por noche`);
      const detail = element(doc, 'a', 'listing-detail-link', 'Ver alojamiento');
      detail.href = buildListingDetailUrl(listing.id);
      card.append(title, location, price, detail);
      items.append(card);
    }
    feedback.textContent = items.children.length ? '' : 'Aún no hay alojamientos sugeridos.';
  } catch {
    if (activeRequests.get(container) !== requestId) return;
    items.replaceChildren();
    feedback.textContent = 'No fue posible cargar las sugerencias. Puedes buscar alojamientos.';
  }
}
