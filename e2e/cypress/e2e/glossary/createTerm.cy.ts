import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossaryView } from '../../compounds/glossaries/E2GlossaryView';

describe('Glossary term creation', () => {
  let data: TestDataStandardResponse;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('Creates a new glossary term', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    const dialog = view.openCreateTermDialog(false);
    dialog.setDefaultTranslation('New Test Term');
    dialog.setDescription('This is a test term description');
    dialog.toggleFlagCaseSensitive();
    dialog.toggleFlagAbbreviation();
    dialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("New Test Term")')
      .should('be.visible');
  });

  it('Creates a non-translatable glossary term', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    const dialog = view.openCreateTermDialog(false);
    dialog.setDefaultTranslation('Non-Translatable Term');
    dialog.toggleFlagNonTranslatable();
    dialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("Non-Translatable Term")')
      .should('be.visible');
  });

  it('Creates a new glossary term when glossary is empty', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Empty Glossary');

    const dialog = view.openCreateTermDialog(true);
    dialog.setDefaultTranslation('New Test Term');
    dialog.setDescription('This is a test term description');
    dialog.toggleFlagForbidden();
    dialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("New Test Term")')
      .should('be.visible');
  });
});
