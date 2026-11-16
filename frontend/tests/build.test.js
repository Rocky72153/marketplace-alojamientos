import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import test from 'node:test';
import { buildFrontend } from '../scripts/build.js';

async function fixture(t) {
  const path = await mkdtemp(join(tmpdir(), 'marketplace-build-'));
  t.after(() => rm(path, { recursive: true, force: true }));
  const root = pathToFileURL(`${path}/`);
  const dist = new URL('dist/', root);
  await mkdir(new URL('src/nested/', root), { recursive: true });
  await writeFile(new URL('index.html', root), '<meta name="api-base-url" content="http://localhost:8080" /><link rel="stylesheet" href="/src/styles.css" /><script type="module" src="/src/main.js"></script>');
  await writeFile(new URL('src/main.js', root), "import { value } from './nested/helper.js';\nconsole.log(value);");
  await writeFile(new URL('src/nested/helper.js', root), 'export const value = 1;');
  await writeFile(new URL('src/styles.css', root), 'body { color: green; }');
  return { root, dist };
}

test('production output versions the whole module graph and stylesheet together', async (t) => {
  const { root, dist } = await fixture(t);
  const { version } = await buildFrontend(root, dist, true);
  const html = await readFile(new URL('index.html', dist), 'utf8');
  assert.match(version, /^[a-f0-9]{16}$/);
  assert.ok(html.includes(`src="/assets/${version}/main.js"`));
  assert.ok(html.includes(`href="/assets/${version}/styles.css"`));
  assert.ok(html.includes('content=""'));
  assert.ok(!html.includes('/src/'));
  assert.equal(await readFile(new URL(`assets/${version}/nested/helper.js`, dist), 'utf8'), 'export const value = 1;');
  assert.match(await readFile(new URL(`assets/${version}/main.js`, dist), 'utf8'), /'\.\/nested\/helper\.js'/);
});

test('the release is deterministic and changes when a nested module or CSS changes', async (t) => {
  const { root, dist } = await fixture(t);
  const first = await buildFrontend(root, dist, false);
  assert.deepEqual(await buildFrontend(root, dist, false), first);
  assert.match(await readFile(new URL('index.html', dist), 'utf8'), /content="http:\/\/localhost:8080"/);
  await writeFile(new URL('src/nested/helper.js', root), 'export const value = 2;');
  const second = await buildFrontend(root, dist, false);
  assert.notEqual(second.version, first.version);
  await writeFile(new URL('src/styles.css', root), 'body { color: blue; }');
  const third = await buildFrontend(root, dist, false);
  assert.notEqual(third.version, second.version);
  await assert.rejects(readFile(new URL(`assets/${first.version}/main.js`, dist)), { code: 'ENOENT' });
});
