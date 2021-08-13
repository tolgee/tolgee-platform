import {
  cleanProjectLeavingData,
  createProjectLeavingData,
  login,
} from '../../common/apiCalls';
import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { selectInProjectMoreMenu } from '../../common/projects';
import { assertMessage, confirmHardMode } from '../../common/shared';

describe('Projects Basics', () => {
  beforeEach(() => {
    cleanProjectLeavingData();
    createProjectLeavingData();
    login('test_username', 'admin');
    cy.visit(`${HOST}`);
  });

  it('cannot leave owning project', () => {
    login('test_username', 'admin');
    cy.visit(`${HOST}`);
    leaveProject('test_project');
    assertMessage(
      'Cannot leave project owned by you. Transfer ownership to other user first.'
    );
  });

  it('cannot leave project with organization role', () => {
    login('pepik', 'admin');
    cy.visit(`${HOST}`);
    leaveProject('Organization owned project');
    assertMessage(
      'Cannot leave project owned by organization you are member of.'
    );
  });

  it('can leave project', () => {
    login('vobtah', 'admin');
    cy.visit(`${HOST}`);
    leaveProject('test_project');
    assertMessage('Project left');
  });

  after(() => {
    cleanProjectLeavingData();
  });
});

const leaveProject = (projectName: string) => {
  selectInProjectMoreMenu(projectName, 'project-leave-button');
  confirmHardMode();
};
