import test from 'node:test'
import assert from 'node:assert/strict'

import { buildIndicatorSystemTreePayload } from '../src/utils/zhpg/indicatorSystemSavePayload.js'

const workModeValues = {
  MAIN_BRANCH_COLLAB: '主分协同',
  INITIAL_DRAFT: 'INITIAL_DRAFT'
}

test('refined tree save keeps the freshly serialized tree instead of restoring stale indicatorTreeWeight', () => {
  const payload = buildIndicatorSystemTreePayload({
    isTemplateMode: false,
    workbenchUsesRefinedTree: true,
    serialized: '{"treeData":{"label":"new","computeRule":{"method":{"node":[]}}}}',
    workModeValues,
    form: {
      id: 43,
      workMode: '主分协同',
      buildPhase: 'REFINED',
      indicatorTree: '{"treeData":{"label":"main"}}',
      indicatorTreeWeight: '{"treeData":{"label":"old-without-compute-rule"}}'
    }
  })

  assert.equal(payload.indicatorTreeWeight, '{"treeData":{"label":"new","computeRule":{"method":{"node":[]}}}}')
  assert.equal(payload.indicatorTree, '{"treeData":{"label":"main"}}')
})
