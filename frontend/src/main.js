import { searchListings, validateSearch } from './search.js';
import { buildListingDetailUrl, fetchListingDetail, renderListingDetail, renderListingDetailError } from './detail.js';
import { initAccount, renderReservationHistory } from './account.js';
import { initBooking } from './booking.js';

if (typeof document !== 'undefined') {
  const searchForm = document.querySelector('#search-form');
  const feedback = document.querySelector('#search-feedback');
  const results = document.querySelector('#search-results');
  const detailSection = document.querySelector('#listing-detail');
  const detailFeedback = document.querySelector('#detail-feedback');
  const detailContent = document.querySelector('#detail-content');
  const apiBase = document.querySelector('meta[name="api-base-url"]')?.content || 'http://localhost:8080';
  const historyContainer = document.querySelector('#reservation-history');
  let currentToken = null;
  let bookingFlow = null;
  const refreshHistory = (notice = '') => renderReservationHistory(historyContainer, apiBase, currentToken, globalThis.fetch,
    document, notice);
  initAccount(document, apiBase, (session) => {
    currentToken = session?.accessToken || null;
    void refreshHistory();
    bookingFlow?.syncAccount();
  });

  const requestedListingId = typeof window === 'undefined'
    ? null
    : new URLSearchParams(window.location.search).get('listing');
  if (requestedListingId && detailSection && detailFeedback && detailContent) {
    detailSection.hidden = false;
    detailFeedback.textContent = 'Cargando alojamiento…';
    fetchListingDetail(requestedListingId, apiBase).then((listing) => {
      renderListingDetail(detailContent, listing);
      bookingFlow = initBooking(detailContent, listing, apiBase, () => currentToken, () => refreshHistory());
      bookingFlow?.syncAccount();
      detailFeedback.textContent = '';
    }).catch((error) => {
      const message = error.message || 'No fue posible consultar este alojamiento.';
      renderListingDetailError(detailContent, message);
      detailFeedback.textContent = '';
    });
  }

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
          const detailLink = document.createElement('a');
          detailLink.className = 'listing-detail-link';
          detailLink.href = buildListingDetailUrl(listing.id);
          detailLink.textContent = 'Ver alojamiento';
          card.append(title, location, price, detailLink);
          results?.append(card);
        }
        const noun = total === 1 ? 'alojamiento' : 'alojamientos';
        const resultLabel = `${total} ${noun} encontrado${total === 1 ? '' : 's'}.`;
        feedback.textContent = total ? resultLabel : 'No encontramos alojamientos con esos criterios.';
      }).catch((error_) => {
        feedback.textContent = error_.message || 'No fue posible consultar los alojamientos.';
        results?.replaceChildren();
      });
    });
  }
}
