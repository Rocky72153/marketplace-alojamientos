import { buildListingDetailUrl } from './detail.js';

const FILTER_NAMES = ['city', 'checkin', 'checkout', 'guests', 'type', 'minPrice', 'maxPrice'];

export function validateSearch({ city, checkin, checkout, guests, minPrice, maxPrice }) {
  if (!city?.trim()) return 'Escribe la ciudad a la que quieres viajar.';
  if (!checkin || !checkout) return 'Selecciona las fechas de llegada y salida.';
  if (checkout <= checkin) return 'La salida debe ser posterior a la llegada.';
  if (!Number.isInteger(Number(guests)) || Number(guests) < 1) {
    return 'El número de huéspedes debe ser al menos 1.';
  }
  if ((minPrice && Number(minPrice) < 0) || (maxPrice && Number(maxPrice) < 0)
      || (minPrice && maxPrice && Number(minPrice) > Number(maxPrice))) {
    return 'El rango de precios no es válido.';
  }
  return '';
}

export function buildSearchUrl(filters, apiBase = 'http://localhost:8080') {
  const params = new URLSearchParams();
  params.set('city', filters.city.trim());
  params.set('checkIn', filters.checkin);
  params.set('checkOut', filters.checkout);
  params.set('guests', String(Number(filters.guests)));
  for (const key of ['type', 'minPrice', 'maxPrice']) if (filters[key]) params.set(key, filters[key]);
  for (const amenity of filters.amenityIds || []) params.append('amenityIds', amenity);
  return `${apiBase.replace(/\/$/, '')}/api/listings?${params.toString()}`;
}

export async function searchListings(filters, apiBase, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(buildSearchUrl(filters, apiBase), { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('No fue posible consultar los alojamientos. Intenta de nuevo.');
  return response.json();
}

function searchParams(query) {
  return query instanceof URLSearchParams ? query : new URLSearchParams(query || '');
}

export function readSearchFilters(query) {
  const params = searchParams(query);
  return { ...Object.fromEntries(FILTER_NAMES.map((key) => [key, params.get(key) || ''])),
    guests: params.get('guests') || '2', amenityIds: params.getAll('amenityIds') };
}

function appendSearchFilters(params, filters, page = 0) {
  for (const key of FILTER_NAMES) {
    const value = String(filters[key] ?? '').trim();
    if (value) params.set(key, value);
    else params.delete(key);
  }
  params.delete('amenityIds');
  for (const amenity of filters.amenityIds || []) params.append('amenityIds', amenity);
  params.delete('searchPage');
  if (Number.isInteger(page) && page > 0) params.set('searchPage', String(page));
  return params;
}

/** Preserve the exact search when opening a listing or returning to its results. */
export function buildSearchDetailUrl(listingId, filters, page = 0) {
  const url = new URL(buildListingDetailUrl(listingId), 'https://marketplace.invalid');
  appendSearchFilters(url.searchParams, filters, page);
  return `${url.pathname}${url.search}`;
}

export function buildSearchReturnUrl(query) {
  const params = searchParams(query);
  if (!params.has('city')) return '/#explorar';
  const page = Number(params.get('searchPage') || 0);
  const result = appendSearchFilters(new URLSearchParams(), readSearchFilters(params), page);
  return `/${result.size ? `?${result}` : ''}#explorar`;
}

export function writeSearchLocation(windowRef, filters, page = 0) {
  if (!windowRef?.history?.replaceState || !windowRef.location?.href) return;
  const url = new URL(windowRef.location.href);
  for (const key of ['listing', 'stripe_checkout', 'stripe_canceled']) url.searchParams.delete(key);
  appendSearchFilters(url.searchParams, filters, page);
  url.hash = 'explorar';
  windowRef.history.replaceState({}, '', url);
}

export function buildCitySuggestionsUrl(query = '', apiBase = 'http://localhost:8080') {
  const params = new URLSearchParams();
  if (query.trim()) params.set('query', query.trim());
  const queryString = params.toString();
  const baseUrl = `${apiBase.replace(/\/$/, '')}/api/listings/cities`;
  return queryString ? `${baseUrl}?${queryString}` : baseUrl;
}

export async function fetchCitySuggestions(query, apiBase) {
  const response = await fetch(buildCitySuggestionsUrl(query, apiBase), { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('No fue posible consultar las ciudades.');
  return response.json();
}
