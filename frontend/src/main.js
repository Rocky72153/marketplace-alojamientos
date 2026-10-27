import { fetchCitySuggestions, searchListings, validateSearch } from './search.js';
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
  const cityInput = document.querySelector('#city');
  const citySuggestions = document.querySelector('#city-suggestions');

  if (cityInput && citySuggestions) {
    let suggestionTimer;
    let requestNumber = 0;
    let activeSuggestion = -1;

    const closeSuggestions = () => {
      requestNumber += 1;
      citySuggestions.hidden = true;
      cityInput.setAttribute('aria-expanded', 'false');
      cityInput.removeAttribute('aria-activedescendant');
      activeSuggestion = -1;
    };

    const chooseSuggestion = (city) => {
      cityInput.value = city;
      closeSuggestions();
    };

    const loadSuggestions = async () => {
      const currentRequest = ++requestNumber;
      try {
        const cities = await fetchCitySuggestions(cityInput.value, apiBase);
        if (currentRequest !== requestNumber) return;
        citySuggestions.replaceChildren();
        for (const [index, city] of cities.slice(0, 10).entries()) {
          const option = document.createElement('li');
          option.id = `city-option-${index}`;
          option.setAttribute('role', 'option');
          option.setAttribute('aria-selected', 'false');
          option.textContent = city;
          option.addEventListener('mousedown', (event) => event.preventDefault());
          option.addEventListener('click', () => chooseSuggestion(city));
          citySuggestions.append(option);
        }
        citySuggestions.hidden = cities.length === 0;
        cityInput.setAttribute('aria-expanded', String(cities.length > 0));
        activeSuggestion = -1;
        cityInput.removeAttribute('aria-activedescendant');
      } catch {
        if (currentRequest === requestNumber) closeSuggestions();
      }
    };

    const setActiveSuggestion = (index) => {
      const options = [...citySuggestions.children];
      if (!options.length) return;
      activeSuggestion = (index + options.length) % options.length;
      options.forEach((option, optionIndex) => option.setAttribute('aria-selected', String(optionIndex === activeSuggestion)));
      cityInput.setAttribute('aria-activedescendant', options[activeSuggestion].id);
    };

    cityInput.setAttribute('role', 'combobox');
    cityInput.setAttribute('aria-autocomplete', 'list');
    cityInput.setAttribute('aria-controls', 'city-suggestions');
    cityInput.setAttribute('aria-expanded', 'false');
    cityInput.setAttribute('autocomplete', 'off');
    cityInput.addEventListener('focus', loadSuggestions);
    cityInput.addEventListener('input', () => {
      requestNumber += 1;
      clearTimeout(suggestionTimer);
      suggestionTimer = setTimeout(loadSuggestions, 180);
    });
    cityInput.addEventListener('keydown', (event) => {
      if (event.key === 'ArrowDown' && !citySuggestions.hidden) {
        event.preventDefault();
        setActiveSuggestion(activeSuggestion + 1);
      } else if (event.key === 'ArrowUp' && !citySuggestions.hidden) {
        event.preventDefault();
        setActiveSuggestion(activeSuggestion < 0 ? citySuggestions.children.length - 1 : activeSuggestion - 1);
      } else if (event.key === 'Enter' && activeSuggestion >= 0) {
        event.preventDefault();
        chooseSuggestion(citySuggestions.children[activeSuggestion].textContent);
      } else if (event.key === 'Escape') {
        closeSuggestions();
      }
    });
    cityInput.addEventListener('blur', () => setTimeout(closeSuggestions, 120));
    void loadSuggestions();
  }
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
