import { fetchCitySuggestions, searchListings, validateSearch } from './search.js';
import { buildListingDetailUrl, fetchListingDetail, renderListingDetail, renderListingDetailError } from './detail.js';
import { initAccount, renderReservationHistory } from './account.js';
import { initBooking } from './booking.js';
import { initNotifications } from './notifications.js';
import { initReviewList, initReviewModeration } from './reviews.js';
import { initAdministration } from './ep09-admin.js';
import { initAccountTools } from './account-tools.js';
import { initHostWorkspace } from './host.js';
import { renderHomeSuggestions } from './home-suggestions.js';
import { fetchPaymentMode, initStripeReturn } from './stripe-checkout.js';
import { initNavigation } from './navigation.js';

function initializeCitySuggestions(cityInput, citySuggestions, apiBase, documentRef) {
  if (!cityInput || !citySuggestions) return Promise.resolve();
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
        const option = documentRef.createElement('li');
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
  return loadSuggestions();
}

if (typeof document !== 'undefined') {
  const searchForm = document.querySelector('#search-form');
  const feedback = document.querySelector('#search-feedback');
  const results = document.querySelector('#search-results');
  const detailSection = document.querySelector('#listing-detail');
  const detailFeedback = document.querySelector('#detail-feedback');
  const detailContent = document.querySelector('#detail-content');
  const apiBase = document.querySelector('meta[name="api-base-url"]')?.content || globalThis.location?.origin || 'http://localhost:8080';
  const cityInput = document.querySelector('#city');
  const citySuggestions = document.querySelector('#city-suggestions');
  const initialSuggestions = initializeCitySuggestions(cityInput, citySuggestions, apiBase, document);
  const historyContainer = document.querySelector('#reservation-history');
  const reservationLink = document.querySelector('#reservation-link');
  const adminPanel = document.querySelector('#review-admin');
  const adminLink = document.querySelector('#admin-reviews-link');
  const adminWorkspace = document.querySelector('#admin-workspace');
  const adminWorkspaceLink = document.querySelector('#admin-workspace-link');
  let currentToken = null;
  let currentRole = null;
  let currentSession = null;
  let accountController = null;
  let bookingFlow = null;
  let reviewList = null;
  let navigation = null;
  const notifications = initNotifications(document.querySelector('#notifications-panel'), apiBase, () => currentToken,
    globalThis.fetch, document);
  const moderation = initReviewModeration(document.querySelector('#review-admin-content'), apiBase, () => currentToken,
    () => reviewList?.refresh(), document, globalThis.fetch);
  const administration = initAdministration(document.querySelector('#admin-workspace-content'), apiBase, () => currentToken,
    document, globalThis.fetch);
  const accountTools = initAccountTools(document.querySelector('#account-tools'), apiBase, {
    onProfileUpdated: (user) => accountController?.updateUser(user),
    onDeactivated: () => accountController?.logout('Tu cuenta fue desactivada.'),
  }, document, globalThis.fetch);
  const hostPanel = document.querySelector('#host-workspace');
  const host = initHostWorkspace(document.querySelector('#host-workspace-content'), apiBase, () => currentSession,
    document, globalThis.fetch);
  document.querySelector('#host-entry')?.addEventListener('click', () => {
    navigation?.navigate('host');
  });
  void renderHomeSuggestions(document.querySelector('#home-suggestions'), apiBase, document, globalThis.fetch);
  const historyCallbacks = {
    getToken: () => currentToken,
    onReservationChanged: () => notifications?.refresh(),
    onReviewCreated: () => reviewList?.refresh(),
  };
  const refreshHistory = (notice = '') => renderReservationHistory(historyContainer, apiBase, currentToken, globalThis.fetch,
    document, notice, historyCallbacks);
  const query = typeof window === 'undefined' ? new URLSearchParams() : new URLSearchParams(window.location.search);
  const paymentReturn = initStripeReturn(document.querySelector('#payment-return'), query.get('stripe_checkout'), apiBase,
    () => currentToken, () => { void refreshHistory(); void notifications?.refresh(); }, document, globalThis.fetch);
  accountController = initAccount(document, apiBase, (session) => {
    const previousToken = currentToken;
    currentSession = session;
    currentToken = session?.accessToken || null;
    currentRole = session?.user?.role || null;
    accountTools?.sync(session);
    if (previousToken !== currentToken) {
      if (hostPanel) hostPanel.hidden = true;
      host?.clear();
    }
    if (adminLink) adminLink.hidden = currentRole !== 'ADMIN';
    if (adminWorkspaceLink) adminWorkspaceLink.hidden = currentRole !== 'ADMIN';
    if (adminWorkspace) adminWorkspace.hidden = true;
    administration?.clear();
    if (reservationLink) reservationLink.hidden = Boolean(session) && currentRole !== 'GUEST';
    const hostLink = document.querySelector('#host-link');
    if (hostLink) {
      hostLink.hidden = Boolean(session) && currentRole !== 'HOST';
      hostLink.textContent = currentRole === 'HOST' ? 'Mis alojamientos' : 'Soy anfitrión';
    }
    if (adminPanel) adminPanel.hidden = true;
    moderation?.clear();
    if (currentRole === 'GUEST') void refreshHistory();
    else void renderReservationHistory(historyContainer, apiBase, null, globalThis.fetch, document);
    if (currentToken) void notifications?.refresh();
    else notifications?.clear();
    bookingFlow?.syncAccount();
    void paymentReturn?.refresh();
    navigation?.syncSession();
  });

  navigation = initNavigation(document, typeof window === 'undefined' ? null : window, () => currentSession, (view) => {
    if (view === 'reservations') void refreshHistory();
    if (view === 'notifications') void notifications?.refresh();
    if (view === 'host') void host?.refresh();
    if (view === 'moderation') void moderation?.refresh();
    if (view === 'admin') void administration?.refresh();
    if (view === 'payment') void paymentReturn?.refresh();
  });

  const requestedListingId = query.get('listing');
  if (requestedListingId && detailSection && detailFeedback && detailContent) {
    let paymentMode = null;
    fetchPaymentMode(apiBase, globalThis.fetch).then((mode) => {
      paymentMode = mode;
      bookingFlow?.syncPaymentMode();
    }).catch(() => {
      paymentMode = 'UNAVAILABLE';
      bookingFlow?.syncPaymentMode();
    });
    detailSection.hidden = false;
    detailFeedback.textContent = 'Cargando alojamiento…';
    fetchListingDetail(requestedListingId, apiBase).then((listing) => {
      renderListingDetail(detailContent, listing);
      bookingFlow = initBooking(detailContent, listing, apiBase, () => currentToken, () => {
        void refreshHistory();
        void notifications?.refresh();
      }, document, globalThis.fetch, { getPaymentMode: () => paymentMode, onRequireAccount: () => navigation?.navigate('account') });
      reviewList = initReviewList(detailContent, listing, apiBase, document, globalThis.fetch);
      bookingFlow?.syncAccount();
      detailFeedback.textContent = '';
    }).catch((error) => {
      const message = error.message || 'No fue posible consultar este alojamiento.';
      renderListingDetailError(detailContent, message);
      detailFeedback.textContent = '';
    });
  }

  if (searchForm && feedback) {
    const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
    let searchRevision = 0;
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
      const revision = ++searchRevision;
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
        if (revision !== searchRevision) return;
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
        if (revision !== searchRevision) return;
        feedback.textContent = error_.message || 'No fue posible consultar los alojamientos.';
        results?.replaceChildren();
      });
    });
  }
  await initialSuggestions;
}
