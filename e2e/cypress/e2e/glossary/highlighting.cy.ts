import {
  getProjectByNameFromTestData,
  glossaryTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('Glossary term highlighting', () => {
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

  it('Highlights glossary terms in default translation when editing non-default translation', () => {
    login('Owner');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_with_term', 'fr').click();
    gcy('glossary-term-highlight').should('exist');
    gcy('glossary-term-highlight').should('contain.text', 'Term');
    view.closeTranslationEdit();

    view.getTranslationCell('key_without_term', 'fr').click();
    gcy('glossary-term-highlight').should('not.exist');
    view.closeTranslationEdit();
  });
});
