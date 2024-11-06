import { formerUserTestData } from '../common/apiCalls/testData/testData';
import { HOST } from '../common/constants';
import { deleteUser, login, setTranslations } from '../common/apiCalls/common';
import { visitTranslations } from '../common/translations';
import { gcyAdvanced } from '../common/shared';

describe('Former user', () => {
  let projectId: number;
  beforeEach(() => {
    formerUserTestData.clean();
    formerUserTestData.generate().then((res) => {
      projectId = res.body.projectId;
      login('will@be.removed').then(() => {
        setTranslations(projectId, 'key', { en: 'Hellooooo!' }).then(() => {
          deleteUser();
        });
      });
    });
    login('admin@admin.com');
  });

  afterEach(() => {
    formerUserTestData.clean();
  });

  it('shows the former user in activity', () => {
    cy.visit(`${HOST}/projects/${projectId}`);
    cy.gcy('project-dashboard-activity-list').should('be.visible');
    cy.contains('Former user').should('be.visible');
  });

  it('shows the former user in translation history', () => {
    visitTranslations(projectId);
    cy.gcy('translations-cell-comments-button').click();
    gcyAdvanced({ value: 'translation-panel-toggle', id: 'history' }).click();
    cy.waitForDom();
    cy.gcy('translation-history-item')
      .findDcy('auto-avatar-img')
      .trigger('mouseover');
    cy.contains('Former user').should('be.visible');
  });

  it('shows the former user in translation comments', () => {
    visitTranslations(projectId);
    cy.gcy('translations-cell-comments-button').click();
    cy.waitForDom();
    cy.gcy('comment').findDcy('auto-avatar-img').trigger('mouseover');
    cy.contains('Former user').should('be.visible');
  });
});
