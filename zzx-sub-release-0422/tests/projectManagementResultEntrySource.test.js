import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/projectManagement/index.vue', import.meta.url), 'utf8')

test('project result entry does not fetch requirement detail just for display', () => {
  assert.doesNotMatch(source, /getEvaluationRequirement/)
  assert.doesNotMatch(source, /evaluationRequirement\/get/)
})

test('project result entry always shows requirement ids before opening results', () => {
  assert.doesNotMatch(source, /rows\.length\s*===\s*1/)
  assert.doesNotMatch(source, /openFormalResult\(rows\[0\]\)/)
})
