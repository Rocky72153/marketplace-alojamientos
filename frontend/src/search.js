export function validateSearch({ city, checkin, checkout, guests }) {
  if (!city.trim()) return 'Escribe la ciudad a la que quieres viajar.';
  if (!checkin || !checkout) return 'Selecciona las fechas de llegada y salida.';
  if (checkout <= checkin) return 'La salida debe ser posterior a la llegada.';
  if (!Number.isInteger(Number(guests)) || Number(guests) < 1) {
    return 'El número de huéspedes debe ser al menos 1.';
  }
  return '';
}
