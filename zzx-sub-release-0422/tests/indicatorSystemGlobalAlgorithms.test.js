import test from 'node:test'
import assert from 'node:assert/strict'

import {
  syncGlobalAlgorithmsToParentNodes,
  removeWeightAssignSubtypeFromParentNodes
} from '../src/utils/zhpg/indicatorSystemGlobalAlgorithms.js'

test('global weight algorithm writes algorithm id without subtype', () => {
  const tree = [{
    label: 'root',
    children: [{ label: 'branch', children: [{ label: 'leaf' }] }]
  }]

  syncGlobalAlgorithmsToParentNodes(tree, {
    weightAssignAlgorithm: '13',
    weightAssignAlgorithmParams: { expertList: ['a'] }
  })

  assert.equal(tree[0].weightAssignAlgorithm, 13)
  assert.equal(tree[0].subtype, undefined)
  assert.deepEqual(tree[0].weightAssignAlgorithmParams, { expertList: ['a'] })
  assert.equal(tree[0].children[0].weightAssignAlgorithm, 13)
  assert.equal(tree[0].children[0].subtype, undefined)
  assert.equal(tree[0].children[0].children[0].weightAssignAlgorithm, undefined)
})

test('global weight algorithm removes stale subtype from parent nodes', () => {
  const tree = [{
    label: 'root',
    subtype: '层次分析',
    children: [{ label: 'leaf' }]
  }]

  syncGlobalAlgorithmsToParentNodes(tree, {
    weightAssignAlgorithm: '21',
    weightAssignAlgorithmParams: { sampleRows: 8 }
  })

  assert.equal(tree[0].weightAssignAlgorithm, 21)
  assert.equal(tree[0].subtype, undefined)
  assert.deepEqual(tree[0].weightAssignAlgorithmParams, { sampleRows: 8 })
})

test('remove weight subtype strips stale subtype from parent nodes only', () => {
  const tree = [{
    label: 'root',
    weightAssignAlgorithm: '13',
    subtype: '层次分析',
    children: [{
      label: 'branch',
      weightAssignAlgorithm: 13,
      subtype: '不校验',
      children: [{ label: 'leaf' }]
    }]
  }]

  removeWeightAssignSubtypeFromParentNodes(tree)

  assert.equal(tree[0].subtype, undefined)
  assert.equal(tree[0].children[0].subtype, undefined)
  assert.equal(tree[0].children[0].children[0].subtype, undefined)
})
