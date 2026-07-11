import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossariesView } from '../../compounds/glossaries/E2GlossariesView';
import { E2GlossaryCreateEditDialog } from '../../compounds/glossaries/E2GlossaryCreateEditDialog';

describe('Glossary', () => {
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

  it('Creates a new glossary', () => {
    login('Owner');
    const view = new E2GlossariesView();
    view.findAndVisit(data, 'Owner');

    const dialog = view.openCreateGlossaryDialog(false);
    fillOutDialog(dialog, true);
    checkGlossaryCreated();
  });

  it('Creates a new glossary from empty state', () => {
    login('Unaffiliated');
    const view = new E2GlossariesView();
    view.findAndVisit(data, 'Unaffiliated');

    const dialog = view.openCreateGlossaryDialog(true);
    fillOutDialog(dialog, false);
    checkGlossaryCreated();
  });

  const fillOutDialog = (
    dialog: E2GlossaryCreateEditDialog,
    assignProject: boolean
  ) => {
    dialog.setName('Create Test Glossary');
    dialog.setBaseLanguage('English');
    if (assignProject) {
      dialog.toggleAssignedProject('TheProject');
    }
    dialog.submit();
  };

  const checkGlossaryCreated = () => {
    gcy('navigation-item')
      .filter(':contains("Create Test Glossary")')
      .should('be.visible');
  };
});
