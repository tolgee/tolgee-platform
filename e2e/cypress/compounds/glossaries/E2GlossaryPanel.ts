import { gcy } from '../../common/shared';


export class E2GlossaryPanel {
  hoverOnTermPreview(name: string) {
    gcy('glossary-term-preview-container')
      .filter(`:contains("${name}")`)
      .first()
      .trigger('mouseover');

    // Wait for edit button to appear in the tooltip
    gcy('glossary-term-preview-edit-button').should('be.visible');
  };
}
