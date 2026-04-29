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
    // PROJECT-type assignments, so the UI shows a single "Always on" chip instead of
    // separate Read/Write chips.
    it('project TM row shows Always on badge (not Read/Write)', () => {
      tmSettings.getTmRows().first().should('contain', 'Always on');
      tmSettings.getTmRows().first().should('not.contain', 'Read');
      tmSettings.getTmRows().first().should('not.contain', 'Write');
    });

    it('shared TM row shows Read and Write badges', () => {
      tmSettings.getTmRows().eq(1).should('contain', 'Read');
      tmSettings.getTmRows().eq(1).should('contain', 'Write');
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
