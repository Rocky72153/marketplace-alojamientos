function element(doc, tag, className, text) {
  const node = doc.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function message(doc) {
  const node = element(doc, 'p', 'review-feedback', '');
  node.setAttribute?.('role', 'status');
  node.setAttribute?.('aria-live', 'polite');
  return node;
}

async function responseBody(response) {
  return response.json().catch(() => ({}));
}

function valid(form) {
  return typeof form.reportValidity !== 'function' || form.reportValidity();
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium', timeZone: 'America/Bogota',
  }).format(date);
}

export function initReviewList(container, listing, apiBase, documentRef = globalThis.document,
  fetchImpl = globalThis.fetch) {
  if (!container || !listing?.id) return null;
  const section = element(documentRef, 'section', 'review-list');
  section.setAttribute?.('aria-labelledby', 'reviews-title');
  const title = element(documentRef, 'h2', '', 'Reseñas de huéspedes');
  title.id = 'reviews-title';
  const feedback = message(documentRef);
  const items = element(documentRef, 'div', 'review-items');
  section.append(title, feedback, items);
  container.append(section);
  let revision = 0;

  async function refresh() {
    const currentRevision = ++revision;
    feedback.textContent = 'Cargando reseñas…';
    try {
      const response = await fetchImpl(new URL(`/api/listings/${encodeURIComponent(listing.id)}/reviews`, apiBase));
      const reviews = await responseBody(response);
      if (currentRevision !== revision) return;
      if (!response.ok || !Array.isArray(reviews)) throw new Error('No fue posible cargar las reseñas.');
      items.replaceChildren();
      if (!reviews.length) {
        feedback.textContent = 'Aún no hay reseñas visibles para este alojamiento.';
        return;
      }
      const average = reviews.reduce((sum, review) => sum + Number(review.rating || 0), 0) / reviews.length;
      feedback.textContent = `Calificación promedio: ${average.toFixed(1)} de 5 · ${reviews.length} reseña(s).`;
      for (const review of reviews) {
        const article = element(documentRef, 'article', 'review-card');
        const heading = element(documentRef, 'h3', '', `${review.guestDisplayName || 'Huésped'} · ${review.rating}/5`);
        const date = element(documentRef, 'p', 'review-date', formatDate(review.createdAt));
        const content = element(documentRef, 'p', 'review-content', review.content || '');
        article.append(heading, date, content);
        items.append(article);
      }
    } catch (error) {
      if (currentRevision !== revision) return;
      items.replaceChildren();
      feedback.textContent = error.message || 'No fue posible cargar las reseñas.';
    }
  }
  refresh();
  return { section, refresh };
}

export function initReviewForm(container, booking, apiBase, getToken, onCreated = () => {},
  documentRef = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container || booking?.status !== 'COMPLETED' || !booking.id || !booking.listingId) return null;
  const section = element(documentRef, 'section', 'review-form-panel');
  section.append(element(documentRef, 'h3', '', 'Escribe tu reseña'));
  const form = element(documentRef, 'form', 'review-form');
  const rating = element(documentRef, 'select');
  rating.name = 'rating'; rating.required = true;
  for (let score = 1; score <= 5; score += 1) {
    const option = element(documentRef, 'option', '', `${score} de 5`);
    option.value = String(score);
    if (score === 5) option.selected = true;
    rating.append(option);
  }
  rating.value = '5';
  const content = element(documentRef, 'textarea');
  content.name = 'content'; content.required = true; content.maxLength = 5000; content.rows = 4;
  const ratingLabel = element(documentRef, 'label', '', 'Calificación');
  ratingLabel.append(rating);
  const contentLabel = element(documentRef, 'label', '', 'Cuenta tu experiencia');
  contentLabel.append(content);
  const submit = element(documentRef, 'button', 'primary-button', 'Publicar reseña');
  submit.type = 'submit';
  const feedback = message(documentRef);
  form.append(ratingLabel, contentLabel, submit);
  section.append(form, feedback);
  container.append(section);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!valid(form)) return;
    const text = content.value?.trim() || '';
    const score = Number(rating.value);
    if (!text || !Number.isInteger(score) || score < 1 || score > 5) {
      feedback.textContent = 'Escribe tu experiencia y elige una calificación entre 1 y 5.';
      return;
    }
    const token = getToken();
    if (!token) {
      feedback.textContent = 'Inicia sesión como huésped para publicar tu reseña.';
      const account = documentRef.querySelector?.('#account-panel');
      if (account) account.hidden = false;
      account?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
      return;
    }
    submit.disabled = true;
    feedback.textContent = 'Publicando reseña…';
    try {
      const response = await fetchImpl(new URL(`/api/listings/${encodeURIComponent(booking.listingId)}/reviews`, apiBase), {
        method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookingId: booking.id, rating: score, content: text }),
      });
      const result = await responseBody(response);
      if (!response.ok) throw new Error(result.message || 'No fue posible publicar la reseña.');
      feedback.textContent = 'Tu reseña fue publicada.';
      form.hidden = true;
      await Promise.resolve(onCreated(result));
    } catch (error) {
      feedback.textContent = error.message || 'No fue posible publicar la reseña.';
      submit.disabled = false;
    }
  });
  return { section };
}

export function initReviewModeration(container, apiBase, getToken, onChanged = () => {},
  documentRef = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container) return null;
  const section = element(documentRef, 'section', 'review-moderation');
  section.append(element(documentRef, 'h2', '', 'Moderación de reseñas'));
  const feedback = message(documentRef);
  const items = element(documentRef, 'div', 'review-moderation-items');
  section.append(feedback, items);
  container.append(section);
  let revision = 0;

  async function refresh() {
    const currentRevision = ++revision;
    const token = getToken();
    if (!token) { items.replaceChildren(); feedback.textContent = 'Inicia sesión como administrador.'; return; }
    section.hidden = false;
    feedback.textContent = 'Cargando reseñas…';
    try {
      const response = await fetchImpl(new URL('/api/admin/reviews', apiBase), { headers: { Authorization: `Bearer ${token}` } });
      const reviews = await responseBody(response);
      if (currentRevision !== revision || getToken() !== token) return;
      if (!response.ok || !Array.isArray(reviews)) throw new Error(reviews.message || 'No fue posible consultar las reseñas.');
      items.replaceChildren();
      feedback.textContent = reviews.length ? `${reviews.length} reseña(s).` : 'Aún no hay reseñas para moderar.';
      for (const review of reviews) {
        const article = element(documentRef, 'article', 'review-card');
        article.append(element(documentRef, 'h3', '', `${review.listingTitle} · ${review.guestDisplayName} · ${review.rating}/5`));
        article.append(element(documentRef, 'p', '', review.content));
        if (review.hidden) {
          article.append(element(documentRef, 'p', '', `Oculta. Motivo: ${review.hiddenReason || 'No disponible'}`));
        } else {
          const form = element(documentRef, 'form', 'review-hide-form');
          const reason = element(documentRef, 'textarea');
          reason.name = 'reason'; reason.required = true; reason.maxLength = 1000; reason.rows = 2;
          const label = element(documentRef, 'label', '', 'Motivo para ocultar');
          label.append(reason);
          const submit = element(documentRef, 'button', '', 'Ocultar reseña');
          submit.type = 'submit';
          const rowFeedback = message(documentRef);
          form.append(label, submit, rowFeedback);
          form.addEventListener('submit', async (event) => {
            event.preventDefault();
            if (!valid(form)) return;
            const value = reason.value?.trim() || '';
            if (!value) { rowFeedback.textContent = 'Indica un motivo para ocultar la reseña.'; return; }
            const currentToken = getToken();
            if (!currentToken) { rowFeedback.textContent = 'Inicia sesión como administrador.'; return; }
            const requestRevision = revision;
            submit.disabled = true;
            try {
              const result = await fetchImpl(new URL(`/api/admin/reviews/${encodeURIComponent(review.id)}/hide`, apiBase), {
                method: 'POST', headers: { Authorization: `Bearer ${currentToken}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason: value }),
              });
              const body = await responseBody(result);
              if (requestRevision !== revision || getToken() !== currentToken) return;
              if (!result.ok) throw new Error(body.message || 'No fue posible ocultar la reseña.');
              await refresh();
              await Promise.resolve(onChanged(body));
            } catch (error) {
              rowFeedback.textContent = error.message || 'No fue posible ocultar la reseña.';
              submit.disabled = false;
            }
          });
          article.append(form);
        }
        items.append(article);
      }
    } catch (error) {
      if (currentRevision !== revision || getToken() !== token) return;
      items.replaceChildren();
      feedback.textContent = error.message || 'No fue posible consultar las reseñas.';
    }
  }
  refresh();
  return { section, refresh, clear() { revision += 1; items.replaceChildren(); feedback.textContent = ''; section.hidden = true; } };
}
