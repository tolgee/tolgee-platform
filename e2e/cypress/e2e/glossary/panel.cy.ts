import {
  getProjectByNameFromTestData,
  glossaryTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { waitForGlobalLoading } from '../../common/loading';

describe('Glossary panel', () => {
  let projectId: number;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'TheProject').id;
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('Shows glossary terms in the panel when editing non-default translation', () => {
    login('Owner');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_with_term', 'fr').click();

    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'glossaries',
    }).click();

    gcy('glossary-panel-container').should('exist');
    gcy('glossary-term-preview-container').should('exist');
    gcy('glossary-term-preview-source-text').should('contain.text', 'Term');
    view.closeTranslationEdit();

    view.getTranslationCell('key_without_term', 'fr').click();
    gcy('glossary-panel-container-empty').should('exist');
    gcy('glossary-term-preview-container').should('not.exist');
    view.closeTranslationEdit();
  });

  describe('Inline editing', () => {
    it('Adds a new translation for a term via panel preview', () => {
      login('Owner');
      const view = new E2TranslationsView();
      view.visit(projectId);

      view.getTranslationCell('key_with_term', 'fr').click();
      const panel = view.openGlossaryPanel();
      panel.hoverOnTermPreview('Term');

      // Click edit button to start editing
      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input').should('be.visible');

      // Type the new French translation
      gcy('glossary-term-preview-edit-input').find('input').type('Terme');

      // Save the translation
      gcy('glossary-term-preview-save-button').click();
      waitForGlobalLoading();

      // Verify the translation was saved
      panel.hoverOnTermPreview('Term');
      gcy('glossary-term-preview-target-text').should('contain.text', 'Terme');
    });

    it('Edits an existing glossary translation via panel preview', () => {
      login('Owner');
      const view = new E2TranslationsView();
      view.visit(projectId);

      view.getTranslationCell('key_with_term', 'fr').click();
      const panel = view.openGlossaryPanel();
      panel.hoverOnTermPreview('Term');

      // First add a translation so we can edit it
      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .type('Terme initial');
      gcy('glossary-term-preview-save-button').click();
      waitForGlobalLoading();

      // Now hover again and edit the existing translation
      panel.hoverOnTermPreview('Term');
      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .should('have.value', 'Terme initial');

      // Clear and type new value
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .clear()
        .type('Terme modifié');
      gcy('glossary-term-preview-save-button').click();
      waitForGlobalLoading();

      // Verify the updated translation
      panel.hoverOnTermPreview('Term');
      gcy('glossary-term-preview-target-text').should(
        'contain.text',
        'Terme modifié'
      );
    });

    it('Saves translation via Enter key', () => {
      login('Owner');
      const view = new E2TranslationsView();
      view.visit(projectId);

      view.getTranslationCell('key_with_term', 'fr').click();
      const panel = view.openGlossaryPanel();
      panel.hoverOnTermPreview('Term');

      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .type('Terme avec Enter{enter}');
      waitForGlobalLoading();

      panel.hoverOnTermPreview('Term');
      gcy('glossary-term-preview-target-text').should(
        'contain.text',
        'Terme avec Enter'
      );
    });

    it('Cancels editing via cancel button', () => {
      login('Owner');
      const view = new E2TranslationsView();
      view.visit(projectId);

      view.getTranslationCell('key_with_term', 'fr').click();
      const panel = view.openGlossaryPanel();
      panel.hoverOnTermPreview('Term');

      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .type('This should be cancelled');

      gcy('glossary-term-preview-cancel-button').click();

      // Edit mode should close
      gcy('glossary-term-preview-edit-input').should('not.exist');
      // Should show empty state since no translation was saved
      panel.hoverOnTermPreview('Term');
      gcy('glossary-term-preview-target-text').should('exist');
    });

    it('Cancels editing via Escape key', () => {
      login('Owner');
      const view = new E2TranslationsView();
      view.visit(projectId);

      view.getTranslationCell('key_with_term', 'fr').click();
      const panel = view.openGlossaryPanel();
      panel.hoverOnTermPreview('Term');

      gcy('glossary-term-preview-edit-button').click();
      gcy('glossary-term-preview-edit-input')
        .find('input')
        .type('This should be cancelled{esc}');

      // Edit mode should close
      gcy('glossary-term-preview-edit-input').should('not.exist');
    });
  });
});
