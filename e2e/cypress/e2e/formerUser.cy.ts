import { formerUserTestData } from '../common/apiCalls/testData/testData';
import { HOST } from '../common/constants';
import { deleteUser, login, setTranslations } from '../common/apiCalls/common';
import { visitTranslations } from '../common/translations';

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
    cy.contains('Project').should('be.visible');
    cy.gcy('former-user-name').should('be.visible');
  });

  it('shows the former user in translation history', () => {
    visitTranslations(projectId);
    cy.gcy('translations-cell-comments-button').click();
    cy.gcy('translations-cell-tab-history').click();
    cy.gcy('translation-history-item')
      .findDcy('auto-avatar-img')
      .trigger('mouseover');
    cy.gcy('former-user-name').should('be.visible');
  });

  it('shows the former user in translation comments', () => {
    visitTranslations(projectId);
    cy.gcy('translations-cell-comments-button').click();
    cy.gcy('comment').findDcy('auto-avatar-img').trigger('mouseover');
    cy.gcy('former-user-name').should('be.visible');
  });
});
