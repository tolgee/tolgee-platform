import 'cypress-file-upload';
import { enterProjectSettings, visitList } from '../../common/projects';
import { assertMessage, gcy } from '../../common/shared';
import { projectTransferringTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Projects Basics', () => {
  beforeEach(() => {
    projectTransferringTestData.clean();
    projectTransferringTestData.generate();
  });

  it('shows proper dialog content', () => {
    login('test_username', 'admin');
    openTransferDialog('test_project');
    gcy('project-transfer-dialog').should(
      'contain',
      'This will transfer the project to another owner.'
    );
    gcy('transfer-project-apply-button').should('be.disabled');
  });

  it('closes transfer dialog', () => {
    login('test_username', 'admin');
    openTransferDialog('test_project');
    gcy('project-transfer-dialog').contains('Cancel').click();
    gcy('project-transfer-dialog').should('not.exist');
  });

  it('transfers to other user', () => {
    login('test_username', 'admin');
    transferProject('test_project', 'Kajetan');
    assertTransferred('test_project', 'Kajetan');
  });

  it('transfers to organization', () => {
    login('test_username', 'admin');
    transferProject('test_project', 'Owned organization');
    assertTransferred('test_project', 'Owned organization');
  });

  it('transfers from organization to user', () => {
    login('test_username', 'admin');
    transferProject('Organization owned project', 'Kajetan');
    assertTransferred('Organization owned project', 'Kajetan', true);
  });

  after(() => {
    projectTransferringTestData.clean();
  });
});

const openTransferDialog = (projectName: string) => {
  enterProjectSettings(projectName);
  gcy('project-settings-transfer-button')
    .should('contain', 'Transfer')
    .wait(100)
    .click();
};

const transferProject = (projectName: string, newOwner: string) => {
  openTransferDialog(projectName);
  gcy('project-transfer-autocomplete-field').find('input').click();
  gcy('project-transfer-autocomplete-suggested-option')
    .contains(newOwner)
    .click();
  gcy('project-transfer-confirmation-field')
    .find('input')
    .type(projectName.toUpperCase());
  gcy('transfer-project-apply-button').click();
};

const assertTransferred = (
  projectName: string,
  newOwner: string,
  missingAfter = false
) => {
  assertMessage('Project transferred');
  visitList();
  if (missingAfter) {
    gcy('dashboard-projects-list-item')
      .contains(projectName)
      .should('not.exist');
    return;
  }
  gcy('dashboard-projects-list-item')
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .findDcy('project-list-owner')
    .should('contain', newOwner);
};
