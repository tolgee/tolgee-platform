type TagFilterField = 'export-tags-in-selector' | 'export-tags-not-in-selector';

export class E2ExportTagFilters {
  addIncludedTag(tag: string) {
    this.addTag('export-tags-in-selector', tag);
  }

  addExcludedTag(tag: string) {
    this.addTag('export-tags-not-in-selector', tag);
  }

  getIncludedTagRemoveButtons() {
    return this.getTagRemoveButtons('export-tags-in-selector');
  }

  getExcludedTagRemoveButtons() {
    return this.getTagRemoveButtons('export-tags-not-in-selector');
  }

  removeOnlyIncludedTag() {
    this.getIncludedTagRemoveButtons().should('have.length', 1).click();
  }

  removeOnlyExcludedTag() {
    this.getExcludedTagRemoveButtons().should('have.length', 1).click();
  }

  getIncludedTagInput() {
    return this.getTagInput('export-tags-in-selector');
  }

  typeIncludedTag(text: string) {
    this.getIncludedTagInput().clear().type(text);
  }

  typeExcludedTag(text: string) {
    this.getTagInput('export-tags-not-in-selector').clear().type(text);
  }

  private addTag(field: TagFilterField, tag: string) {
    this.getTagInput(field).clear().type(tag);
    cy.gcy('tag-autocomplete-option').should('have.length', 1).click();
  }

  private getTagInput(field: TagFilterField) {
    return cy.gcy(field).findDcy('tag-autocomplete-input');
  }

  private getTagRemoveButtons(field: TagFilterField) {
    return cy.gcy(field).findDcy('translations-tag-close');
  }
}
