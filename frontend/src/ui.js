/** Progressive enhancement: native links and forms remain usable without the menu. */
export function initShell(doc, win = globalThis.window) {
  const toggle = doc.querySelector('#menu-toggle');
  const nav = doc.querySelector('#primary-navigation');
  const close = () => {
    toggle?.setAttribute('aria-expanded', 'false');
    nav?.removeAttribute('data-open');
  };
  if (toggle && nav) {
    doc.body?.setAttribute('data-enhanced', 'true');
    toggle.addEventListener('click', () => {
      const open = toggle.getAttribute('aria-expanded') !== 'true';
      toggle.setAttribute('aria-expanded', String(open));
      if (open) nav.setAttribute('data-open', '');
      else nav.removeAttribute('data-open');
    });
    nav.addEventListener('click', (event) => {
      if (event.target.closest?.('a')) close();
    });
    doc.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
        close();
        toggle.focus();
      }
    });
    win?.addEventListener('popstate', close);
  }
  doc.querySelector('.skip-link')?.addEventListener('click', (event) => {
    event.preventDefault();
    const main = doc.querySelector('#main-content');
    main?.focus({ preventScroll: true });
    main?.scrollIntoView?.({ block: 'start' });
  });
  for (const button of doc.querySelectorAll?.('[data-destination]') || []) {
    button.addEventListener('click', () => {
      const city = doc.querySelector('#city');
      if (!city) return;
      city.value = button.getAttribute('data-destination');
      city.dispatchEvent(new (doc.defaultView?.Event || Event)('input', { bubbles: true }));
      doc.querySelector('#checkin')?.focus();
      doc.querySelector('#search-form')?.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
    });
  }
}
