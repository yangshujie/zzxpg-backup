import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('external data client uses the huage-api dev proxy configured by this project', async () => {
  const source = await readFile(new URL('../src/api/zhpg/externalData.js', import.meta.url), 'utf8')

  assert.match(source, /baseURL:\s*['"]\/huage-api['"]/)
  assert.doesNotMatch(source, /baseURL:\s*['"]\/external-data-api['"]/)
})
