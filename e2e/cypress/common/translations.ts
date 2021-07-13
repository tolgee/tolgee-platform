export function getCellEditButton(content: string) {
  return cy
    .contains(content)
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-edit-button']"
    )
    .invoke('show');
}

export function getCellCancelButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-cancel-button']"
    );
}

export function getCellSaveButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-save-button']"
    );
}
