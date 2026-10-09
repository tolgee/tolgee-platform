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
    return cy.gcy('export-tags-in-selector').findDcy('tag-autocomplete-input');
  }

  typeIncludedTag(text: string) {
    this.getIncludedTagInput().clear().type(text);
  }

  private addTag(field: TagFilterField, tag: string) {
    cy.gcy(field).findDcy('tag-autocomplete-input').type(tag);
    cy.gcy('tag-autocomplete-option').should('have.length', 1).click();
  }

  private getTagRemoveButtons(field: TagFilterField) {
    return cy.gcy(field).findDcy('translations-tag-close');
  }
}
