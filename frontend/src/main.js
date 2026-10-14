import { validateSearch } from './search.js';

if (typeof document !== 'undefined') {
  const searchForm = document.querySelector('#search-form');
  const feedback = document.querySelector('#search-feedback');

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
      const data = Object.fromEntries(new FormData(searchForm).entries());
      const error = validateSearch(data);
      feedback.textContent = error || 'La búsqueda estará disponible cuando se integre el servicio de alojamientos.';
    });
  }
}
