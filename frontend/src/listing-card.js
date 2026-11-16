const AMENITIES = { WIFI: 'Wi-Fi', PARKING: 'Parqueadero', POOL: 'Piscina', AIR_CONDITIONING: 'Aire acondicionado' };
const TYPES = { APARTMENT: 'Apartamento', HOUSE: 'Casa', ROOM: 'Habitación', CABIN: 'Cabaña' };
const ILLUSTRATIONS = { APARTMENT: 'apartment', HOUSE: 'house', ROOM: 'room', CABIN: 'cabin' };

export const amenityLabel = (value) => Object.hasOwn(AMENITIES, value) ? AMENITIES[value] : value;
export const listingTypeLabel = (value) => Object.hasOwn(TYPES, value) ? TYPES[value] : 'Alojamiento';

export function formatListingPrice(value) {
  const amount = Number(value);
  if (value == null || value === '' || !Number.isFinite(amount) || amount < 0) return 'Precio no disponible';
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', minimumFractionDigits: Number.isInteger(amount) ? 0 : 2, maximumFractionDigits: 2,
  }).format(amount);
}

/** Accept web image references only. Missing photographs use an explicitly labelled local illustration. */
export function safeListingImage(value) {
  if (typeof value !== 'string' || !value.trim()) return null;
  try {
    const url = new URL(value, 'https://marketplace.invalid');
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) return null;
    return value.trim();
  } catch { return null; }
}

export function listingImageReferences(listing) {
  return [...new Set((Array.isArray(listing.imageReferences) ? listing.imageReferences : [])
    .map(safeListingImage).filter(Boolean))];
}

export function listingCapacity(value) {
  const capacity = Number(value);
  return Number.isInteger(capacity) && capacity > 0
    ? `Hasta ${capacity} ${capacity === 1 ? 'huésped' : 'huéspedes'}` : 'Capacidad por confirmar';
}

function element(doc, tag, className, text) {
  const node = doc.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

export function createPhotoPlaceholder(doc, className = 'listing-photo-placeholder', message = 'Sin fotos publicadas', type = 'HOUSE') {
  const placeholder = element(doc, 'div', className);
  const image = element(doc, 'img', 'listing-photo-illustration');
  const illustration = Object.hasOwn(ILLUSTRATIONS, type) ? ILLUSTRATIONS[type] : 'house';
  image.src = new URL(`./assets/listing-${illustration}.svg`, import.meta.url).href;
  image.alt = ''; image.loading = 'lazy'; image.decoding = 'async';
  const labels = element(doc, 'div', 'listing-photo-labels');
  labels.append(element(doc, 'span', 'listing-photo-tag', 'Ilustración'),
    element(doc, 'span', 'listing-photo-message', message));
  placeholder.append(image, labels);
  return placeholder;
}

export function renderListingCard(doc, listing, { href } = {}) {
  const title = listing.title || 'Alojamiento';
  const target = href || `/?listing=${encodeURIComponent(listing.id)}`;
  const card = element(doc, 'article', 'listing-card');
  const media = element(doc, 'a', 'listing-card-media');
  media.href = target;
  media.setAttribute?.('aria-label', `Ver alojamiento: ${title}`);
  const placeholder = createPhotoPlaceholder(doc, 'listing-photo-placeholder', 'Sin fotos publicadas', listing.type);
  const reference = listingImageReferences(listing)[0];
  if (reference) {
    const image = element(doc, 'img', 'listing-card-image');
    image.src = reference; image.alt = `Fotografía de ${title}`;
    image.loading = 'lazy'; image.decoding = 'async'; image.referrerPolicy = 'no-referrer';
    placeholder.hidden = true;
    image.addEventListener?.('error', () => { image.hidden = true; placeholder.hidden = false; });
    media.append(image);
  }
  media.append(placeholder, element(doc, 'span', 'listing-card-type', listingTypeLabel(listing.type)));
  const body = element(doc, 'div', 'listing-card-body');
  const heading = element(doc, 'h3', 'listing-card-title');
  const titleLink = element(doc, 'a', 'listing-card-title-link', title); titleLink.href = target;
  heading.append(titleLink);
  body.append(heading, element(doc, 'p', 'listing-card-location', listing.city || 'Ubicación por confirmar'),
    element(doc, 'p', 'listing-card-capacity', listingCapacity(listing.capacity)));
  const amenities = [...new Set(Array.isArray(listing.amenityIds) ? listing.amenityIds : [])];
  if (amenities.length) {
    const services = element(doc, 'ul', 'listing-card-amenities');
    services.setAttribute?.('aria-label', 'Servicios destacados');
    for (const value of amenities.slice(0, 3)) services.append(element(doc, 'li', '', amenityLabel(value)));
    if (amenities.length > 3) services.append(element(doc, 'li', 'listing-card-amenities-more', `+${amenities.length - 3} más`));
    body.append(services);
  } else body.append(element(doc, 'p', 'listing-card-services-note', 'Consulta los servicios en la ficha'));
  const footer = element(doc, 'div', 'listing-card-footer');
  const price = element(doc, 'p', 'listing-price', formatListingPrice(listing.pricePerNight));
  if (listing.pricePerNight != null && listing.pricePerNight !== '' && Number.isFinite(Number(listing.pricePerNight)) && Number(listing.pricePerNight) >= 0) {
    price.append(element(doc, 'span', 'listing-price-unit', ' COP / noche'));
  }
  const detail = element(doc, 'a', 'listing-detail-link', 'Ver alojamiento'); detail.href = target;
  footer.append(price, detail); card.append(media, body, footer);
  return card;
}
