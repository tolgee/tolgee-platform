import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { gcy } from '../../common/shared';
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

  after(() => {
    projectTestData.clean();
  });
});
