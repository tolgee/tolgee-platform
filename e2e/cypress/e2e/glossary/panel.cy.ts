import {
  getProjectByNameFromTestData,
  glossaryTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

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
});
