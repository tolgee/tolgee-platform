import 'cypress-file-upload';
import { translationMemoryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2TranslationMemoriesView } from '../../compounds/translationMemories/E2TranslationMemoriesView';
import { E2TranslationMemoryView } from '../../compounds/translationMemories/E2TranslationMemoryView';
import { gcy } from '../../common/shared';

describe('Translation Memory empty-state wizard', () => {
  let data: TestDataStandardResponse;
  const listView = new E2TranslationMemoriesView();
  const tmView = new E2TranslationMemoryView();

  // Test fixture mapping (matches TranslationMemoryTestData):
  //   - "Unassigned Shared TM"   → empty SHARED TM, source=en        (the wizard target)
  //   - "Shared Marketing TM"    → SHARED TM with 3 entries          (no wizard expected)
  //   - "Project With TM"        → project with en→de translations   (copy-from-project source)

  beforeEach(() => {
    translationMemoryTestData.clean();
    translationMemoryTestData.generateStandard().then((res) => {
      data = res.body;
      login('test_username');
    });
  });

  afterEach(() => {
    translationMemoryTestData.clean();
  });

  it('renders the wizard on an empty TM', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEmptyWizard().should('be.visible');
    tmView.getEmptyWizardManualCard().should('be.visible');
    tmView.getEmptyWizardCopyCard().should('be.visible');
    tmView.getEmptyWizardImportCard().should('be.visible');
    tmView.getEntryRows().should('not.exist');
  });

  it('does not render the wizard on a populated TM', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEmptyWizard().should('not.exist');
    tmView.getEntryRows().should('have.length.at.least', 1);
  });

  it('manual card opens the new-entry dialog', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEmptyWizardManualCard().click();
    gcy('tm-create-entry-dialog').should('be.visible');
  });

  it('import card opens the TMX import dialog', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEmptyWizardImportCard().click();
    gcy('tm-import-dialog').should('be.visible');
  });

  it('sync card opens the manage-projects dialog', () => {
    // Sync card opens the project-only TM settings dialog so the user can connect projects
    // (write-assignments) and pull their translations in as virtual content.
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEmptyWizardCopyCard().click();
    gcy('tm-settings-dialog').should('be.visible');
  });
});
