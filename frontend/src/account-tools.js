function element(documentRef, tag, text, className = '') {
  const node = documentRef.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function input(documentRef, labelText, name, type = 'text') {
  const label = element(documentRef, 'label', labelText);
  const field = element(documentRef, 'input');
  field.name = name;
  field.type = type;
  label.append(field);
  return { label, field };
}

function feedbackNode(documentRef) {
  const node = element(documentRef, 'p', '', 'feedback');
  node.setAttribute('role', 'status');
  node.setAttribute('aria-live', 'polite');
  return node;
}

async function request(fetchImpl, url, options) {
  const response = await fetchImpl(url, options);
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || 'No fue posible completar la operación.');
  return body;
}

/**
 * Account settings for the signed-in person and simulated recovery when signed out.
 * The caller owns the session; callbacks must update or clear it after a successful change.
 */
export function initAccountTools(container, apiBase, callbacks = {},
  documentRef = globalThis.document, fetchImpl = globalThis.fetch) {
  if (!container) return null;
  let session = null;
  let revision = 0;

  const profile = element(documentRef, 'section', undefined, 'account-form');
  profile.append(element(documentRef, 'h3', 'Mi perfil'));
  const identity = element(documentRef, 'p', '');
  const form = element(documentRef, 'form');
  const name = input(documentRef, 'Nombre', 'name');
  name.field.required = true; name.field.maxLength = 120; name.field.autocomplete = 'name';
  const phone = input(documentRef, 'Teléfono', 'phone', 'tel');
  phone.field.maxLength = 40; phone.field.autocomplete = 'tel';
  const photo = input(documentRef, 'Referencia de foto', 'photoReference');
  photo.field.maxLength = 1000;
  const save = element(documentRef, 'button', 'Guardar cambios'); save.type = 'submit';
  form.append(name.label, phone.label, photo.label, save);
  const profileFeedback = feedbackNode(documentRef);
  const deactivate = element(documentRef, 'button', 'Desactivar mi cuenta', 'secondary-button');
  deactivate.type = 'button';
  const confirmation = element(documentRef, 'div');
  confirmation.hidden = true;
  confirmation.append(element(documentRef, 'p', 'Al desactivar tu cuenta se cerrará la sesión y no podrás iniciar sesión con ella.'));
  const confirm = element(documentRef, 'button', 'Confirmar desactivación');
  confirm.type = 'button';
  const cancel = element(documentRef, 'button', 'Cancelar', 'secondary-button');
  cancel.type = 'button';
  confirmation.append(confirm, cancel);
  profile.append(identity, form, profileFeedback, deactivate, confirmation);

  const recovery = element(documentRef, 'details', undefined, 'account-form');
  recovery.append(element(documentRef, 'summary', 'Solicitar recuperación simulada'));
  recovery.append(element(documentRef, 'p', 'Esta demostración no envía correos ni modifica tu contraseña.'));
  const recoveryForm = element(documentRef, 'form');
  const email = input(documentRef, 'Correo', 'email', 'email');
  email.field.required = true; email.field.maxLength = 320; email.field.autocomplete = 'email';
  const submit = element(documentRef, 'button', 'Solicitar recuperación'); submit.type = 'submit';
  const recoveryFeedback = feedbackNode(documentRef);
  recoveryForm.append(email.label, submit);
  recovery.append(recoveryForm, recoveryFeedback);
  container.append(profile, recovery);

  const current = (token, requestRevision) => Boolean(token) && revision === requestRevision
    && session?.accessToken === token;
  function resetProfile() {
    identity.textContent = '';
    name.field.value = ''; phone.field.value = ''; photo.field.value = '';
    profileFeedback.textContent = '';
    confirmation.hidden = true;
    save.disabled = false; confirm.disabled = false; deactivate.disabled = false;
  }
  async function load(token, requestRevision) {
    profileFeedback.textContent = 'Cargando tu perfil…';
    try {
      const data = await request(fetchImpl, new URL('/api/profile/me', apiBase), {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!current(token, requestRevision)) return;
      identity.textContent = `${data.email || ''} · ${data.role || ''}`;
      name.field.value = data.name || '';
      phone.field.value = data.phone || '';
      photo.field.value = data.photoReference || '';
      profileFeedback.textContent = '';
    } catch (error) {
      if (current(token, requestRevision)) profileFeedback.textContent = error.message || 'No fue posible consultar tu perfil.';
    }
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const token = session?.accessToken;
    const requestRevision = revision;
    const updatedName = name.field.value.trim();
    if (!token) return;
    if (!updatedName) { profileFeedback.textContent = 'Escribe tu nombre.'; return; }
    save.disabled = true;
    profileFeedback.textContent = 'Guardando…';
    try {
      const data = await request(fetchImpl, new URL('/api/profile/me', apiBase), {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: updatedName, phone: phone.field.value.trim(), photoReference: photo.field.value.trim() }),
      });
      if (!current(token, requestRevision)) return;
      name.field.value = data.name || '';
      phone.field.value = data.phone || '';
      photo.field.value = data.photoReference || '';
      identity.textContent = `${data.email || ''} · ${data.role || ''}`;
      profileFeedback.textContent = 'Perfil actualizado.';
      callbacks.onProfileUpdated?.(data);
    } catch (error) {
      if (current(token, requestRevision)) profileFeedback.textContent = error.message || 'No fue posible actualizar tu perfil.';
    } finally {
      if (current(token, requestRevision)) save.disabled = false;
    }
  });

  deactivate.addEventListener('click', () => { confirmation.hidden = false; });
  cancel.addEventListener('click', () => { confirmation.hidden = true; });
  confirm.addEventListener('click', async () => {
    const token = session?.accessToken;
    const requestRevision = revision;
    if (!token) return;
    confirm.disabled = true;
    profileFeedback.textContent = 'Desactivando tu cuenta…';
    try {
      await request(fetchImpl, new URL('/api/profile/me', apiBase), {
        method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
      });
      if (!current(token, requestRevision)) return;
      callbacks.onDeactivated?.();
    } catch (error) {
      if (current(token, requestRevision)) profileFeedback.textContent = error.message || 'No fue posible desactivar tu cuenta.';
    } finally {
      if (current(token, requestRevision)) confirm.disabled = false;
    }
  });

  recoveryForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const requestRevision = revision;
    const address = email.field.value.trim();
    if (!address) { recoveryFeedback.textContent = 'Escribe tu correo.'; return; }
    submit.disabled = true;
    recoveryFeedback.textContent = 'Procesando…';
    try {
      await request(fetchImpl, new URL('/api/auth/recovery', apiBase), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: address }),
      });
      if (revision !== requestRevision || session) return;
      recoveryFeedback.textContent = 'Solicitud simulada registrada. No se ha enviado un correo ni se ha cambiado la contraseña.';
      email.field.value = '';
    } catch (error) {
      if (revision === requestRevision && !session) recoveryFeedback.textContent = error.message || 'No fue posible registrar la solicitud.';
    } finally {
      if (revision === requestRevision && !session) submit.disabled = false;
    }
  });

  function sync(nextSession) {
    if (nextSession?.accessToken && nextSession.accessToken === session?.accessToken) {
      session = nextSession;
      return;
    }
    revision += 1;
    session = nextSession?.accessToken ? nextSession : null;
    resetProfile();
    recoveryFeedback.textContent = '';
    email.field.value = '';
    submit.disabled = false;
    profile.hidden = !session;
    recovery.hidden = Boolean(session);
    if (session) void load(session.accessToken, revision);
  }

  sync(null);
  return { sync, clear: () => sync(null) };
}
