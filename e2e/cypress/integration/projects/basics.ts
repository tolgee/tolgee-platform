import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { assertMessage, gcy } from '../../common/shared';
import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Projects Basics', () => {
  beforeEach(() => {
    projectTestData.clean();
    projectTestData.generate();
    login('cukrberg@facebook.com', 'admin');
    cy.visit(`${HOST}`);
  });

  it('Searches in list', () => {
    gcy('global-list-search').find('input').type('Facebook');
    gcy('global-paginated-list')
      .within(() =>
        gcy('dashboard-projects-list-item').should('have.length', 1)
      )
      .contains('Facebook itself');
  });

  it('Creates project with user owner', () => {
    createProject('I am a great project', 'Mark Cukrberg');
  });

  it('Creates with organization owner', () => {
    createProject('I am a great project', 'Facebook');
  });

  const createProject = (name: string, owner: string) => {
    gcy('global-plus-button').click();
    gcy('project-owner-select').click();
    gcy('project-owner-select-item').contains(owner).click();
    gcy('project-name-field').find('input').type(name);
    gcy('global-form-save-button').click();
    assertMessage('Project created');
    gcy('global-paginated-list')
      .contains(name)
      .closestDcy('dashboard-projects-list-item')
      .within(() => {
        gcy('project-list-owner').contains(owner).should('be.visible');
      });
  };

  after(() => {
    projectTestData.clean();
  });
});
