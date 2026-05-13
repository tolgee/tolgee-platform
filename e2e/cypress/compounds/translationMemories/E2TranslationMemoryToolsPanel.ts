export class E2TranslationMemoryToolsPanel {
  getItems() {
    return cy.gcy('translation-tools-translation-memory-item');
  }

  // Each item-scoped helper re-fetches the item internally — Cypress chains are mutable,
  // so reusing one chain across multiple `findDcy` calls walks deeper into the DOM after
  // the first traversal. Taking the locator text here keeps each call independent.
  getItemContaining(text: string) {
    return this.getItems().filter(`:contains("${text}")`);
  }

  getEmptyState() {
    return cy.contains('Nothing found');
  }

  getScoreOf(text: string) {
    return this.getItemContaining(text).findDcy(
      'translation-tools-translation-memory-item-score'
    );
  }

  getMetaTmNameOf(text: string) {
    return this.getItemContaining(text).findDcy(
      'translation-tools-translation-memory-item-tm-name'
    );
  }

  getMetaKeyNameOf(text: string) {
    return this.getItemContaining(text).findDcy(
      'translation-tools-translation-memory-item-key-name'
    );
  }

  getMetaUpdatedAtOf(text: string) {
    return this.getItemContaining(text).findDcy(
      'translation-tools-translation-memory-item-updated'
    );
  }

  hoverScoreOf(text: string) {
    return this.getScoreOf(text).trigger('mouseover');
  }

  // MUI tooltip portals into the document root — read it via role.
  getTooltip() {
    return cy.get('[role="tooltip"]');
  }
}
