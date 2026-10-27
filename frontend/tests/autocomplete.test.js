import test from 'node:test';
import assert from 'node:assert/strict';

test('al cargar la página muestra destinos sin sesión y permite elegir con teclado', async () => {
  const previous = { document: globalThis.document, fetch: globalThis.fetch };
  const handlers = {};
  const attributes = {};
  const cityInput = {
    value: '',
    setAttribute(name, value) { attributes[name] = value; },
    removeAttribute(name) { delete attributes[name]; },
    addEventListener(name, handler) { handlers[name] = handler; },
  };
  const options = [];
  const citySuggestions = {
    hidden: true,
    get children() { return options; },
    replaceChildren() { options.length = 0; },
    append(option) { options.push(option); },
  };
  globalThis.document = {
    querySelector(selector) {
      if (selector === '#city') return cityInput;
      if (selector === '#city-suggestions') return citySuggestions;
      return null;
    },
    createElement(tagName) {
      const optionAttributes = {};
      return {
        tagName,
        setAttribute(name, value) { optionAttributes[name] = value; },
        getAttribute(name) { return optionAttributes[name]; },
        addEventListener(name, handler) { this[`on${name}`] = handler; },
      };
    },
  };
  globalThis.fetch = async (url) => {
    assert.match(String(url), /\/api\/listings\/cities$/);
    return { ok: true, async json() { return ['Bogotá D.C., Bogotá D.C.', 'Medellín, Antioquia']; } };
  };

  try {
    await import('../src/main.js?initial-city-suggestions-test');
    await new Promise(setImmediate);
    assert.equal(citySuggestions.hidden, false);
    assert.equal(attributes['aria-expanded'], 'true');
    assert.equal(options.length, 2);

    let prevented = false;
    handlers.keydown({ key: 'ArrowDown', preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(options[0].getAttribute('aria-selected'), 'true');

    handlers.keydown({ key: 'Enter', preventDefault() { prevented = true; } });
    assert.equal(cityInput.value, 'Bogotá D.C., Bogotá D.C.');
    assert.equal(citySuggestions.hidden, true);
  } finally {
    globalThis.document = previous.document;
    globalThis.fetch = previous.fetch;
  }
});
