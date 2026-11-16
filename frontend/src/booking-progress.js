/** A visual and screen-reader-readable guide; it does not assert payment status. */
export function createBookingProgress(doc, initialMode = 'SIMULATED_TEST') {
  const nav = doc.createElement('nav');
  nav.className = 'booking-progress';
  nav.setAttribute?.('aria-label', 'Progreso de la reserva');
  const list = doc.createElement('ol');
  const steps = Array.from({ length: 4 }, () => doc.createElement('li'));
  list.replaceChildren(...steps);
  nav.replaceChildren(list);
  function setStep(index, mode = initialMode) {
    const labels = ['Fechas', 'Cotización', mode === 'STRIPE_TEST' ? 'Stripe de prueba' : 'Pago de prueba', 'Confirmación'];
    steps.forEach((item, i) => {
      const state = i < index ? 'complete' : i === index ? 'current' : 'upcoming';
      item.textContent = `${i + 1}. ${labels[i]}`;
      item.setAttribute?.('data-state', state);
      item.setAttribute?.('aria-label', `Paso ${i + 1} de 4: ${labels[i]}, ${state === 'current' ? 'actual' : state === 'complete' ? 'completado' : 'pendiente'}`);
      if (state === 'current') item.setAttribute?.('aria-current', 'step');
      else item.removeAttribute?.('aria-current');
    });
  }
  setStep(0);
  return { element: nav, setStep };
}
