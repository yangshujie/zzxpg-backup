function asText(value) {
  return value == null ? '' : String(value).trim()
}

export function normalizeTableCreationRecord(record) {
  const row = record && typeof record === 'object' ? record : {}
  const sourceSystem = asText(row.subCenterCode) || asText(row.sourceSystem)
  const sourceSystemName = asText(row.subCenterName) || asText(row.sourceSystemName) || sourceSystem
  const tableName = asText(row.tableCode) || asText(row.tableName)
  const tableDisplayName = asText(row.tableName) || asText(row.tableDisplayName) || tableName
  const fieldName = asText(row.fieldCode) || asText(row.fieldName)
  const fieldDisplayName = asText(row.fieldComment) || asText(row.fieldName) || asText(row.fieldDisplayName) || fieldName
  const fieldComment = asText(row.fieldComment) || fieldDisplayName || fieldName

  return {
    ...row,
    sourceSystem,
    sourceSystemName,
    tableName,
    tableDisplayName,
    fieldName,
    fieldDisplayName,
    fieldComment,
    fieldType: asText(row.fieldType)
  }
}

export function normalizeTableCreationRecords(records) {
  return Array.isArray(records) ? records.map(normalizeTableCreationRecord) : []
}
