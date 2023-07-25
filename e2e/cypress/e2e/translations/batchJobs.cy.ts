import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { batchJobs } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import { deleteSelected } from '../../common/groupActions';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { login } from '../../common/apiCalls/common';

describe('Batch jobs', () => {
  let project: TestDataStandardResponse['projects'][number] = null;

  beforeEach(() => {
    batchJobs.clean();
    batchJobs.generateStandard().then((data) => {
      project = data.body.projects[0];
      login('admin');
      visit();

      // wait for loading to appear and disappear again
      cy.gcy('global-base-view-content').should('be.visible');
      waitForGlobalLoading();
    });
  });

  it('will delete all properly', () => {
    gcy('translations-row-checkbox').first().click();
    gcy('translations-select-all-button').click();
    waitForGlobalLoading(500);
    deleteSelected();
    waitForGlobalLoading(500);
    gcy('global-empty-list').should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
