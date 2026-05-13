import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeTableCreationRecord } from '../src/utils/zhpg/externalDataRecord.js'

test('normalizeTableCreationRecord maps latest backend codes into frontend fields', () => {
  const row = normalizeTableCreationRecord({
    id: 105,
    subCenterName: '航天发射特征实验分析分中心',
    subCenterCode: 'space_launch',
    tableName: 'link_truth_series',
    tableCode: 'link_truth_series',
    fieldName: '误码率BER',
    fieldCode: 'ber',
    fieldType: 'long',
    fieldComment: '误码率BER'
  })

  assert.equal(row.sourceSystem, 'space_launch')
  assert.equal(row.sourceSystemName, '航天发射特征实验分析分中心')
  assert.equal(row.tableName, 'link_truth_series')
  assert.equal(row.tableDisplayName, 'link_truth_series')
  assert.equal(row.fieldName, 'ber')
  assert.equal(row.fieldDisplayName, '误码率BER')
  assert.equal(row.fieldComment, '误码率BER')
  assert.equal(row.fieldType, 'long')
})

test('normalizeTableCreationRecord keeps legacy field names as fallback', () => {
  const row = normalizeTableCreationRecord({
    id: 7,
    sourceSystem: 'legacy_center',
    tableName: 'legacy_table',
    fieldName: 'speed',
    fieldComment: '速度'
  })

  assert.equal(row.sourceSystem, 'legacy_center')
  assert.equal(row.tableName, 'legacy_table')
  assert.equal(row.fieldName, 'speed')
  assert.equal(row.fieldComment, '速度')
  assert.equal(row.fieldDisplayName, '速度')
  assert.equal(row.fieldType, '')
})
