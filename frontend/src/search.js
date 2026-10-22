export function validateSearch({ city, checkin, checkout, guests, minPrice, maxPrice }) {
  if (!city.trim()) return 'Escribe la ciudad a la que quieres viajar.';
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

export async function searchListings(filters, apiBase) {
  const response = await fetch(buildSearchUrl(filters, apiBase), { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('No fue posible consultar los alojamientos. Intenta de nuevo.');
  return response.json();
}
