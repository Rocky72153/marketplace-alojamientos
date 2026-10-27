const AMENITY_LABELS = {
  WIFI: 'Wi-Fi',
  PARKING: 'Parqueadero',
  POOL: 'Piscina',
  AIR_CONDITIONING: 'Aire acondicionado',
};

const TYPE_LABELS = {
  APARTMENT: 'Apartamento',
  HOUSE: 'Casa',
  ROOM: 'Habitación',
  CABIN: 'Cabaña',
};

export function buildListingDetailUrl(listingId) {
  if (!listingId) throw new Error('El alojamiento no tiene un identificador válido.');
  return `/?listing=${encodeURIComponent(listingId)}`;
}

export async function fetchListingDetail(listingId, apiBase, fetchImpl = globalThis.fetch) {
  if (!listingId) throw new Error('No se indicó qué alojamiento consultar.');
  const url = new URL(`/api/listings/${encodeURIComponent(listingId)}`, apiBase);
  const response = await fetchImpl(url);
  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('Este alojamiento no está disponible para consulta pública.');
    }
    throw new Error('No fue posible consultar este alojamiento. Intenta de nuevo.');
  }
  return response.json();
}

function addText(parent, tagName, className, text, documentRef) {
  const element = documentRef.createElement(tagName);
  if (className) element.className = className;
  element.textContent = text;
  parent.append(element);
  return element;
}

function formatPrice(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return 'Precio no disponible';
  return new Intl.NumberFormat('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  }).format(amount);
}

function safeImageReference(reference) {
  if (typeof reference !== 'string' || !reference.trim()) return false;
  try {
    const url = new URL(reference, 'https://marketplace.invalid');
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export function renderListingDetail(container, listing, documentRef = globalThis.document) {
  container.replaceChildren();
  const article = documentRef.createElement('article');
  article.className = 'detail-card';

  const gallery = documentRef.createElement('div');
  gallery.className = 'detail-gallery';
  const imageReferences = (listing.imageReferences || []).filter(safeImageReference);
  if (imageReferences.length) {
    for (const [index, reference] of imageReferences.entries()) {
      const image = documentRef.createElement('img');
      image.src = reference;
      image.alt = `${listing.title || 'Alojamiento'} · imagen ${index + 1}`;
      image.loading = 'lazy';
      gallery.append(image);
    }
  } else {
    addText(gallery, 'p', 'detail-no-image', 'Este alojamiento aún no tiene imágenes disponibles.', documentRef);
  }

  const content = documentRef.createElement('div');
  content.className = 'detail-copy';
  addText(content, 'p', 'eyebrow', 'DETALLE DEL ALOJAMIENTO', documentRef);
  const title = addText(content, 'h1', 'detail-title', listing.title || 'Alojamiento', documentRef);
  title.id = 'detail-title';
  const location = [listing.location, listing.city].filter(Boolean).join(' · ');
  addText(content, 'p', 'detail-location', location || 'Ubicación no disponible', documentRef);
  addText(content, 'p', 'detail-description', listing.description || 'Sin descripción disponible.', documentRef);
  addText(content, 'p', 'detail-price', `${formatPrice(listing.pricePerNight)} por noche`, documentRef);

  const facts = documentRef.createElement('dl');
  facts.className = 'detail-facts';
  const factValues = [
    ['Tipo', TYPE_LABELS[listing.type] || listing.type || 'No especificado'],
    ['Capacidad', `${listing.capacity ?? 'No especificada'} huéspedes`],
    ['Anfitrión', listing.hostName || 'No especificado'],
  ];
  for (const [label, value] of factValues) {
    addText(facts, 'dt', '', label, documentRef);
    addText(facts, 'dd', '', value, documentRef);
  }
  content.append(facts);

  const amenities = Array.isArray(listing.amenityIds) ? listing.amenityIds : [];
  addText(content, 'h2', 'detail-subheading', 'Servicios', documentRef);
  addText(content, 'p', 'detail-amenities', amenities.length
    ? amenities.map((amenity) => AMENITY_LABELS[amenity] || amenity).join(' · ')
    : 'No se especificaron servicios.', documentRef);
  addText(content, 'h2', 'detail-subheading', 'Reglas del alojamiento', documentRef);
  addText(content, 'p', 'detail-rules', listing.rules || 'El anfitrión no indicó reglas.', documentRef);

  article.append(gallery, content);
  container.append(article);
}

export function renderListingDetailError(container, message, documentRef = globalThis.document) {
  container.replaceChildren();
  const error = addText(container, 'p', 'detail-error', message, documentRef);
  error.role = 'status';
}
