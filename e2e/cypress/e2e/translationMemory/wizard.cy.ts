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

  it('copy card seeds the TM from a project and dismisses the wizard', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEmptyWizardCopyCard().click();
    gcy('tm-empty-wizard-copy-dialog').should('be.visible');

    tmView.copyFromProject('Project With TM');

    // Two virtual entries on Project With TM (existingKey + reviewedKey, both with German
    // targets) get copied into the empty shared TM as manual entries. Wizard should give
    // way to the populated table.
    tmView.getEmptyWizard().should('not.exist');
    tmView.getEntryRowContaining('Existing source').should('be.visible');
    tmView.getEntryRowContaining('Reviewed source').should('be.visible');
  });

  it('toolbar menu can copy from a project on a non-empty TM (idempotent)', () => {
    // Copy-from-project also works on a TM that already has entries — the backend skips
    // duplicates by (sourceText, targetLanguageTag, targetText), so existing entries stay
    // intact and only new ones land. "Shared Marketing TM" has 2 distinct source texts;
    // pulling in Project With TM (2 different sources) should grow it to 4.
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length', 2);

    tmView.openCopyFromProjectDialogFromMenu();
    tmView.copyFromProject('Project With TM');

    tmView.getEntryRowContaining('Existing source').should('be.visible');
    tmView.getEntryRowContaining('Reviewed source').should('be.visible');
    tmView.getEntryRowContaining('Hello world').should('be.visible');
    tmView.getEntryRowContaining('Thank you').should('be.visible');
  });
});
