import { amenityLabel, createPhotoPlaceholder, formatListingPrice, listingCapacity,
  listingImageReferences, listingTypeLabel } from './listing-card.js';

export function buildListingDetailUrl(listingId) {
  if (!listingId) throw new Error('El alojamiento no tiene un identificador válido.');
  return `/?listing=${encodeURIComponent(listingId)}`;
}

export async function fetchListingDetail(listingId, apiBase, fetchImpl = globalThis.fetch) {
  if (!listingId) throw new Error('No se indicó qué alojamiento consultar.');
  const url = new URL(`/api/listings/${encodeURIComponent(listingId)}`, apiBase);
  const response = await fetchImpl(url);
  if (!response.ok) {
    if (response.status === 404) throw new Error('Este alojamiento no está disponible para consulta pública.');
    throw new Error('No fue posible consultar este alojamiento. Intenta de nuevo.');
  }
  return response.json();
}

function node(doc, tag, className, text) {
  const value = doc.createElement(tag);
  if (className) value.className = className;
  if (text !== undefined) value.textContent = text;
  return value;
}

function detailSection(doc, id, title, className = '') {
  const section = node(doc, 'section', `detail-info-section ${className}`.trim());
  const heading = node(doc, 'h2', 'detail-subheading', title); heading.id = id;
  section.setAttribute?.('aria-labelledby', id);
  section.append(heading);
  return section;
}

function renderGallery(doc, listing) {
  const gallery = node(doc, 'div', 'detail-gallery');
  const references = listingImageReferences(listing);
  if (!references.length) {
    gallery.append(createPhotoPlaceholder(doc, 'detail-no-image', 'Sin fotos publicadas de este alojamiento', listing.type));
    return gallery;
  }
  const stage = node(doc, 'figure', 'detail-gallery-stage');
  const image = node(doc, 'img', 'detail-main-image'); image.id = 'listing-main-image';
  image.loading = 'eager'; image.decoding = 'async'; image.referrerPolicy = 'no-referrer';
  const fallback = createPhotoPlaceholder(doc, 'detail-no-image', 'No se pudo cargar esta fotografía', listing.type);
  fallback.hidden = true;
  const caption = node(doc, 'figcaption', 'detail-gallery-caption');
  caption.setAttribute?.('aria-live', 'polite'); caption.setAttribute?.('aria-atomic', 'true');
  stage.append(image, fallback, caption); gallery.append(stage);
  const buttons = [];
  let active = 0;
  function select(index, focus = false) {
    active = (index + references.length) % references.length;
    image.hidden = false; fallback.hidden = true;
    image.src = references[active]; image.alt = `${listing.title || 'Alojamiento'} · fotografía ${active + 1} de ${references.length}`;
    caption.textContent = `Fotografía ${active + 1} de ${references.length}`;
    buttons.forEach((button, buttonIndex) => button.setAttribute?.('aria-pressed', String(buttonIndex === active)));
    if (focus) buttons[active]?.focus?.();
  }
  image.addEventListener?.('error', () => { image.hidden = true; fallback.hidden = false; });
  if (references.length > 1) {
    const thumbnails = node(doc, 'div', 'detail-gallery-thumbnails');
    thumbnails.setAttribute?.('role', 'group'); thumbnails.setAttribute?.('aria-label', 'Elegir fotografía del alojamiento');
    references.forEach((reference, index) => {
      const button = node(doc, 'button', 'detail-thumbnail'); button.type = 'button';
      button.setAttribute?.('aria-label', `Ver fotografía ${index + 1} de ${references.length}`);
      button.setAttribute?.('aria-controls', 'listing-main-image');
      const thumbnail = node(doc, 'img'); thumbnail.src = reference; thumbnail.alt = ''; thumbnail.loading = 'lazy';
      thumbnail.referrerPolicy = 'no-referrer';
      const number = node(doc, 'span', 'detail-thumbnail-number', String(index + 1)); number.setAttribute?.('aria-hidden', 'true');
      thumbnail.addEventListener?.('error', () => { thumbnail.hidden = true; });
      button.append(thumbnail, number);
      button.addEventListener?.('click', () => select(index));
      button.addEventListener?.('keydown', (event) => {
        const keys = { ArrowLeft: active - 1, ArrowRight: active + 1, Home: 0, End: references.length - 1 };
        if (!Object.hasOwn(keys, event.key)) return;
        event.preventDefault(); select(keys[event.key], true);
      });
      buttons.push(button); thumbnails.append(button);
    });
    gallery.append(thumbnails);
  }
  select(0);
  return gallery;
}

function renderCancellation(doc, policy) {
  const section = detailSection(doc, 'listing-cancellation-title', 'Política de cancelación', 'detail-cancellation');
  const free = Number(policy?.freeRefundDays); const partial = Number(policy?.partialRefundDays);
  const percentage = Number(policy?.partialRefundPercentage);
  if (!policy || ['freeRefundDays', 'partialRefundDays', 'partialRefundPercentage'].some((key) => policy[key] == null || policy[key] === '')
      || !Number.isInteger(free) || !Number.isInteger(partial) || free < partial || partial < 0
      || !Number.isFinite(percentage) || percentage < 0 || percentage > 100) {
    section.append(node(doc, 'p', '', 'Consulta la política vigente y el importe de tu reserva al calcular la cotización.'));
    return section;
  }
  const list = node(doc, 'ul', 'detail-cancellation-rules');
  list.append(node(doc, 'li', '', `Reembolso del 100 % con ${free} días o más de anticipación.`));
  if (free > partial) list.append(node(doc, 'li', '', `Si no aplica el reembolso total, reembolso del ${percentage} % con al menos ${partial * 24} horas de anticipación.`));
  if (partial > 0) list.append(node(doc, 'li', '', `Sin reembolso con menos de ${partial * 24} horas de anticipación.`));
  section.append(list, node(doc, 'p', 'detail-policy-note', 'Se aplica según la fecha de llegada en horario de Colombia. La cotización confirma la política de tu reserva.'));
  return section;
}

export function renderListingDetail(container, listing, documentRef = globalThis.document) {
  container.replaceChildren();
  const article = node(documentRef, 'article', 'detail-card');
  const content = node(documentRef, 'div', 'detail-copy');
  content.append(node(documentRef, 'p', 'eyebrow', listingTypeLabel(listing.type)));
  const title = node(documentRef, 'h1', 'detail-title', listing.title || 'Alojamiento'); title.id = 'detail-title';
  const location = [listing.location, listing.city].filter(Boolean).join(' · ');
  const price = formatListingPrice(listing.pricePerNight);
  content.append(title, node(documentRef, 'p', 'detail-location', location || 'Ubicación por confirmar'),
    node(documentRef, 'p', 'detail-price', price === 'Precio no disponible' ? price : `${price} COP por noche`),
    node(documentRef, 'p', 'detail-price-note', 'Tarifa base. Consulta el total con limpieza, servicio y extras en tu cotización.'));
  const bookingJump = node(documentRef, 'button', 'secondary-button detail-booking-jump', 'Ver fechas y cotizar');
  bookingJump.type = 'button';
  bookingJump.addEventListener?.('click', () => {
    const booking = documentRef.querySelector?.('.booking-panel');
    booking?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
    booking?.querySelector?.('h2')?.focus?.({ preventScroll: true });
  });
  content.append(bookingJump);
  const facts = node(documentRef, 'dl', 'detail-facts');
  for (const [label, value] of [['Tipo', listingTypeLabel(listing.type)], ['Capacidad', listingCapacity(listing.capacity)],
    ['Anfitrión', listing.hostName || 'Información no disponible']]) {
    facts.append(node(documentRef, 'dt', '', label), node(documentRef, 'dd', '', value));
  }
  content.append(facts);
  const description = detailSection(documentRef, 'listing-description-title', 'Acerca de este alojamiento');
  description.append(node(documentRef, 'p', 'detail-description', listing.description || 'El anfitrión aún no ha añadido una descripción.'));
  const services = detailSection(documentRef, 'listing-amenities-title', 'Servicios disponibles');
  const amenities = [...new Set(Array.isArray(listing.amenityIds) ? listing.amenityIds : [])];
  if (amenities.length) {
    const list = node(documentRef, 'ul', 'detail-amenities');
    for (const value of amenities) list.append(node(documentRef, 'li', 'detail-amenity', amenityLabel(value)));
    services.append(list);
  } else services.append(node(documentRef, 'p', 'detail-amenities-empty', 'El anfitrión no ha especificado servicios.'));
  const rules = detailSection(documentRef, 'listing-rules-title', 'Reglas del alojamiento');
  rules.append(node(documentRef, 'p', 'detail-rules', listing.rules || 'El anfitrión no ha indicado reglas.'));
  content.append(description, services, rules, renderCancellation(documentRef, listing.cancellationPolicy));
  if (Array.isArray(listing.extraOptions) && listing.extraOptions.length) {
    const extras = detailSection(documentRef, 'listing-extras-title', 'Extras opcionales', 'detail-extras');
    const list = node(documentRef, 'ul', 'detail-extras-list');
    for (const extra of listing.extraOptions) {
      const amount = formatListingPrice(extra.unitPrice);
      list.append(node(documentRef, 'li', '', `${extra.name} · ${amount}${amount === 'Precio no disponible' ? '' : ' COP por unidad'}`));
    }
    extras.append(list); content.append(extras);
  }
  article.append(renderGallery(documentRef, listing), content); container.append(article);
}

export function renderListingDetailError(container, message, documentRef = globalThis.document) {
  container.replaceChildren();
  const error = node(documentRef, 'p', 'detail-error', message); error.role = 'status'; container.append(error);
}
