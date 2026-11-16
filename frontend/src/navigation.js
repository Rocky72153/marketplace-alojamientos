const HASH_VIEWS = {
  explorar: 'explore', buscar: 'explore', 'mi-cuenta': 'account', 'crear-cuenta': 'account',
  reservas: 'reservations', 'reservation-history': 'reservations', notificaciones: 'notifications',
  'notifications-panel': 'notifications', anfitrion: 'host', anfitriones: 'host',
  administracion: 'admin', 'admin-workspace': 'admin', moderacion: 'moderation',
  'review-admin': 'moderation', alojamiento: 'detail', pago: 'payment',
};
const VIEW_HASHES = {
  explore: 'explorar', account: 'mi-cuenta', reservations: 'reservas', notifications: 'notificaciones',
  host: 'anfitrion', admin: 'administracion', moderation: 'moderacion', detail: 'alojamiento', payment: 'pago',
};
const PANELS = {
  account: 'account-panel', reservations: 'reservation-history', notifications: 'notifications-panel',
  host: 'host-workspace', admin: 'admin-workspace', moderation: 'review-admin', detail: 'listing-detail', payment: 'payment-return',
};

export function resolveView(location = {}) {
  const query = new URLSearchParams(location.search || '');
  const hash = (location.hash || '').replace(/^#/, '');
  const hashView = Object.hasOwn(HASH_VIEWS, hash) ? HASH_VIEWS[hash] : null;
  const view = hashView || (query.has('stripe_checkout') ? 'payment' : query.has('listing') ? 'detail' : 'explore');
  if (view === 'detail' && !query.has('listing')) return 'explore';
  if (view === 'payment' && !query.has('stripe_checkout')) return 'explore';
  return view;
}

export function permittedView(requested, session) {
  if (['reservations', 'notifications', 'host', 'admin', 'moderation'].includes(requested) && !session) return 'account';
  if (requested === 'host' && session?.user?.role !== 'HOST') return 'account';
  if (['admin', 'moderation'].includes(requested) && session?.user?.role !== 'ADMIN') return 'account';
  if (requested === 'reservations' && session?.user?.role !== 'GUEST') return 'account';
  return requested;
}

/** Wrappers own visibility; asynchronous feature modules only modify their contents. */
export function initNavigation(doc, win, getSession, onViewChange = () => {}) {
  const wrappers = Object.keys(VIEW_HASHES).map((view) => [view, doc.querySelector(`#view-${view}`)]);
  const links = [...(doc.querySelectorAll?.('[data-view-link]') || [])];
  const tabs = [...(doc.querySelectorAll?.('[data-auth-tab]') || [])];
  let authTab = 'login';
  let activeView;

  function selectAuthTab(tab) {
    authTab = tab === 'register' ? 'register' : 'login';
    const login = doc.querySelector('#login-form');
    const register = doc.querySelector('#register-form');
    if (login) login.hidden = authTab !== 'login';
    if (register) register.hidden = authTab !== 'register';
    for (const item of tabs) {
      const selected = item.getAttribute('data-auth-tab') === authTab;
      item.setAttribute('aria-selected', String(selected));
      item.setAttribute('tabindex', selected ? '0' : '-1');
    }
  }

  function apply({ focus = false } = {}) {
    const session = getSession();
    const requested = resolveView(win?.location);
    const view = permittedView(requested, session);
    const context = new URLSearchParams(win?.location?.search || '');
    const listingReturn = doc.querySelector('#account-listing-return');
    const paymentReturn = doc.querySelector('#account-payment-return');
    if (listingReturn) listingReturn.hidden = !context.get('listing');
    if (paymentReturn) paymentReturn.hidden = !context.get('stripe_checkout');
    for (const [name, wrapper] of wrappers) if (wrapper) wrapper.hidden = name !== view;
    if (PANELS[view]) {
      const panel = doc.querySelector(`#${PANELS[view]}`);
      if (panel) panel.hidden = false;
    }
    doc.body?.setAttribute?.('data-view', view);
    for (const link of links) {
      if (link.getAttribute('data-view-link') === view) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    }
    const authTabs = doc.querySelector('#auth-tabs');
    if (authTabs) authTabs.hidden = Boolean(session);
    if (view === 'account') {
      if (win?.location?.hash === '#crear-cuenta') selectAuthTab('register');
      else selectAuthTab(authTab);
      const title = doc.querySelector('#account-title');
      if (title) title.textContent = session ? 'Mi cuenta' : 'Tu próximo viaje empieza aquí';
      if (requested !== view) {
        const feedback = doc.querySelector('#account-feedback');
        if (feedback) feedback.textContent = !session ? 'Inicia sesión para continuar.' : 'Esta sección no está disponible para tu tipo de cuenta.';
        if (requested === 'host' && !session) {
          const role = doc.querySelector('#register-form select[name="role"]');
          if (role) role.value = 'HOST';
        }
      }
    }
    const changed = activeView !== view;
    activeView = view;
    if (changed) onViewChange(view);
    if (focus) {
      const wrapper = doc.querySelector(`#view-${view}`);
      wrapper?.focus?.({ preventScroll: true });
      win?.scrollTo?.({ top: 0, behavior: 'instant' });
    }
    return view;
  }

  function navigate(view) {
    if (!Object.hasOwn(VIEW_HASHES, view)) return;
    const url = new URL(win?.location?.href || 'http://localhost/');
    if (view !== 'account' || activeView !== 'account') {
      url.searchParams.delete('resume_booking');
      url.searchParams.delete('resume_payment');
    }
    if (view === 'account' && activeView === 'payment' && !getSession() && url.searchParams.get('stripe_checkout')) {
      url.searchParams.set('resume_payment', '1');
    }
    if (view === 'explore') { url.searchParams.delete('listing'); url.searchParams.delete('stripe_checkout'); url.searchParams.delete('stripe_canceled'); }
    url.hash = VIEW_HASHES[view];
    if (win?.history?.pushState) win.history.pushState({}, '', url);
    else if (win?.location) win.location.hash = url.hash;
    apply({ focus: true });
  }

  function requireAccountForBooking() {
    const url = new URL(win?.location?.href || 'http://localhost/');
    if (!url.searchParams.get('listing')) return navigate('account');
    url.searchParams.delete('resume_payment');
    url.searchParams.set('resume_booking', '1');
    url.hash = VIEW_HASHES.account;
    if (win?.history?.pushState) win.history.pushState({}, '', url);
    else if (win?.location) win.location.hash = url.hash;
    apply({ focus: true });
  }

  function syncSession() {
    const url = new URL(win?.location?.href || 'http://localhost/');
    // A payment return takes precedence if a manually edited URL contains both markers.
    if (url.searchParams.get('resume_payment') === '1' && url.searchParams.get('stripe_checkout')
        && resolveView(win?.location) === 'account' && getSession()) {
      url.searchParams.delete('resume_payment');
      url.searchParams.delete('resume_booking');
      url.hash = VIEW_HASHES.payment;
      if (win?.history?.replaceState) win.history.replaceState(win.history.state, '', url);
      else if (win?.location) win.location.hash = url.hash;
      return apply({ focus: true });
    }
    if (url.searchParams.get('resume_booking') === '1' && url.searchParams.get('listing')
        && resolveView(win?.location) === 'account' && getSession()?.user?.role === 'GUEST') {
      url.searchParams.delete('resume_booking');
      url.searchParams.delete('resume_payment');
      url.hash = VIEW_HASHES.detail;
      if (win?.history?.replaceState) win.history.replaceState(win.history.state, '', url);
      else if (win?.location) win.location.hash = url.hash;
      return apply({ focus: true });
    }
    const view = apply();
    if (url.searchParams.get('resume_booking') === '1' && view === 'account' && getSession()
        && getSession()?.user?.role !== 'GUEST') {
      const feedback = doc.querySelector('#account-feedback');
      if (feedback) feedback.textContent = 'Para reservar, inicia sesión con una cuenta de huésped.';
    }
    return view;
  }

  for (const link of links) link.addEventListener('click', (event) => {
    event.preventDefault();
    navigate(link.getAttribute('data-view-link'));
  });
  for (const tab of tabs) {
    tab.addEventListener('click', () => selectAuthTab(tab.getAttribute('data-auth-tab')));
    tab.addEventListener('keydown', (event) => {
      if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
      event.preventDefault();
      selectAuthTab(authTab === 'login' ? 'register' : 'login');
      tabs.find((item) => item.getAttribute('data-auth-tab') === authTab)?.focus?.();
    });
  }
  win?.addEventListener?.('hashchange', () => apply({ focus: true }));
  win?.addEventListener?.('popstate', () => apply({ focus: true }));
  selectAuthTab('login');
  syncSession();
  return { navigate, requireAccountForBooking, syncSession, getView: () => activeView, selectAuthTab };
}
