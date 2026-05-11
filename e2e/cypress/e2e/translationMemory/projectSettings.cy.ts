import { translationMemoryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2ProjectTmSettings } from '../../compounds/translationMemories/E2ProjectTmSettings';
import { E2TranslationMemoriesView } from '../../compounds/translationMemories/E2TranslationMemoriesView';

describe('Translation Memory project settings', () => {
  let data: TestDataStandardResponse;
  const tmSettings = new E2ProjectTmSettings();
  const tmListView = new E2TranslationMemoriesView();

  beforeEach(() => {
    translationMemoryTestData.clean();
    translationMemoryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    translationMemoryTestData.clean();
  });

  it('shows translation memory section on Advanced tab', () => {
    login('test_username');
    tmSettings.findAndVisit(data, 'Project With TM');

    tmSettings.getSharedSection().should('be.visible');
  });

  describe('Connected TMs table', () => {
    beforeEach(() => {
      login('test_username');
      tmSettings.findAndVisit(data, 'Project With TM');
    });

    it('lists project and shared TMs with type badges', () => {
      tmSettings.getTmRows().should('have.length.at.least', 2);
      tmSettings.getSharedSection().should('contain', 'Project only');
      tmSettings.getSharedSection().should('contain', 'Shared');
    });

    it('shows priority numbers', () => {
      tmSettings.getTable().should('be.visible');
      tmSettings.getTmRows().first().should('contain', '1');
      tmSettings.getTmRows().eq(1).should('contain', '2');
    });

    // First row (priority 0) is the project TM — read/write are hard-coded to true for
    // PROJECT-type assignments, so the UI shows a single "Always on" chip instead of any
    // access-state badge.
    it('project TM row shows Always on badge (not Read/Write)', () => {
      tmSettings.getTmRows().first().should('contain', 'Always on');
      tmSettings.getTmRows().first().should('not.contain', 'Read');
      tmSettings.getTmRows().first().should('not.contain', 'Write');
    });

    // sharedTm's assignment to projectWithTm has writeAccess=false in the fixture, so the
    // access badge collapses to "Read-only".
    it('shared TM row shows the collapsed Read-only badge when only read is granted', () => {
      tmSettings.getTmRows().eq(1).should('contain', 'Read-only');
      tmSettings.getTmRows().eq(1).should('not.contain', 'Write');
    });
  });

  it('manage all TMs button navigates to org TM settings', () => {
    login('test_username');
    tmSettings.findAndVisit(data, 'Project With TM');

    tmSettings.clickManageAllTms();

    cy.url().should('include', '/translation-memories');
    tmListView.getListItems().should('have.length.at.least', 1);
  });
});
