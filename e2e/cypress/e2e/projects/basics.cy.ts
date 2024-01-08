import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { gcy, switchToOrganization } from '../../common/shared';
import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { createProject } from '../../common/projects';

describe('Projects Basics', () => {
  beforeEach(() => {
    projectTestData.clean();
    projectTestData.generate();
    login('cukrberg@facebook.com', 'admin');
    cy.visit(`${HOST}`);
  });

  it('Searches in list', () => {
    switchToOrganization('Microsoft');
    gcy('global-list-search').find('input').type('Microsoft Word');
    gcy('global-paginated-list')
      .within(() =>
        gcy('dashboard-projects-list-item').should('have.length', 1)
      )
      .contains('Microsoft Word');
  });

  it('Search is hidden for less than 5 projects', () => {
    switchToOrganization('Facebook');
    gcy('global-list-search').should('not.exist');
  });

  it('Creates with organization owner', () => {
    createProject('I am a great project', 'Facebook');
  });

  after(() => {
    projectTestData.clean();
  });
});
