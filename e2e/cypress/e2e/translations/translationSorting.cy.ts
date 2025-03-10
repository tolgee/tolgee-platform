import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';

import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

const TIMEOUT_ONE_MINUTE = 1000 * 60;

describe('Translations sorting', () => {
  let project: ProjectDTO = null;

  before(() => {
    translationsTestData.cleanupForFilters();
    translationsTestData
      .generateForFilters()
      .then((p) => {
        project = p;
      })
      .then(() => {
        login('franta', 'admin');
        visit();
      });
  });

  beforeEach(() => {
    login('franta', 'admin');
    visit();
    waitForGlobalLoading();
    cy.contains('A key', { timeout: TIMEOUT_ONE_MINUTE }).should('be.visible');
  });

  after(() => {
    translationsTestData.cleanupForFilters();
  });

  it('sort by key name', () => {
    cy.gcy('translation-controls-order-item').click();
    cy.wait(1000000);
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
