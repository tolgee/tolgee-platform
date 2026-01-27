import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

export class E2GlossaryPanel {
  hoverOnTermPreview(name: string) {
    gcy('glossary-term-preview-container')
      .filter(`:contains("${name}")`)
      .first()
      .trigger('mouseover');

    // Wait for the edit button to appear in the tooltip
    gcy('glossary-term-preview-edit-button').should('be.visible');
  }

  inlineEditTermPreview(
    name: string,
    newTranslation: string,
    currentTranslation: string | undefined = undefined
  ) {
    // Hover over the term preview to show the edit button
    this.hoverOnTermPreview(name);

    gcy('glossary-term-preview-edit-button').click();
    gcy('glossary-term-preview-edit-input').should('be.visible');
    if (currentTranslation !== undefined) {
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .should('have.value', currentTranslation);
    }
    gcy('glossary-term-preview-edit-input')
      .find('input')
      .clear()
      .type(newTranslation);
    gcy('glossary-term-preview-save-button').click();
    waitForGlobalLoading();
  }
}
