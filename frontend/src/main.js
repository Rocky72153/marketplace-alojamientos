import { searchListings, validateSearch } from './search.js';

if (typeof document !== 'undefined') {
  const searchForm = document.querySelector('#search-form');
  const feedback = document.querySelector('#search-feedback');
  const results = document.querySelector('#search-results');
  const apiBase = document.querySelector('meta[name="api-base-url"]')?.content || 'http://localhost:8080';

  if (searchForm && feedback) {
    const today = new Date().toISOString().slice(0, 10);
    const checkin = document.querySelector('#checkin');
    const checkout = document.querySelector('#checkout');
    checkin.min = today;
    checkout.min = today;
    checkin.addEventListener('change', () => {
      checkout.min = checkin.value || today;
      if (checkout.value && checkout.value <= checkin.value) checkout.value = '';
    });
    searchForm.addEventListener('submit', (event) => {
      event.preventDefault();
      const formData = new FormData(searchForm);
      const data = Object.fromEntries(formData.entries());
      data.amenityIds = formData.getAll('amenityIds');
      const error = validateSearch(data);
      if (error) {
        feedback.textContent = error;
        results?.replaceChildren();
        return;
      }
      feedback.textContent = 'Buscando alojamientos…';
      searchListings(data, apiBase).then(({ items = [], total = 0 }) => {
        results?.replaceChildren();
        for (const listing of items) {
          const card = document.createElement('article');
          card.className = 'listing-card';
          const title = document.createElement('h3');
          title.textContent = listing.title;
          const location = document.createElement('p');
          location.textContent = `${listing.city} · ${listing.type} · Hasta ${listing.capacity} huéspedes`;
          const price = document.createElement('p');
          price.className = 'listing-price';
          price.textContent = `$${Number(listing.pricePerNight).toLocaleString('es-CO')} por noche`;
          card.append(title, location, price);
          results?.append(card);
        }
        feedback.textContent = total ? `${total} alojamiento${total === 1 ? '' : 's'} encontrado${total === 1 ? '' : 's'}.` : 'No encontramos alojamientos con esos criterios.';
      }).catch((failure) => {
        feedback.textContent = failure.message || 'No fue posible consultar los alojamientos.';
        results?.replaceChildren();
      });
    });
  }
}
