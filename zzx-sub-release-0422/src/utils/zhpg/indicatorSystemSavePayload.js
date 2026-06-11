export function buildIndicatorSystemTreePayload({
  isTemplateMode,
  workbenchUsesRefinedTree,
  form,
  serialized,
  workModeValues
}) {
  const payload = {
    indicatorTree: serialized
  }

  if (isTemplateMode) {
    if (form.id == null) {
      payload.isTemplate = 1
    }
    return payload
  }

  payload.buildPhase =
    form.workMode === workModeValues.MAIN_BRANCH_COLLAB
      ? form.buildPhase || workModeValues.INITIAL_DRAFT
      : form.buildPhase

  if (form.id == null) {
    payload.isTemplate = 0
  }

  return payload
}
