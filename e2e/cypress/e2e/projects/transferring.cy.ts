import 'cypress-file-upload';
import { enterProjectSettings, visitList } from '../../common/projects';
import { assertMessage, gcy, switchToOrganization } from '../../common/shared';
import { projectTransferringTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';

describe('Projects Transferring', () => {
  beforeEach(() => {
    projectTransferringTestData.clean();
    projectTransferringTestData.generate();
  });

  it('shows proper dialog content', { retries: { runMode: 5 } }, () => {
    login('test_username', 'admin');
    openTransferDialog('Organization owned project', 'Owned organization');
    gcy('project-transfer-dialog').should(
      'contain',
      'This will transfer the project to another owner.'
    );
    gcy('transfer-project-apply-button').should('be.disabled');
  });

  it('closes transfer dialog', () => {
    login('test_username', 'admin');
    openTransferDialog('Organization owned project', 'Owned organization');
    gcy('project-transfer-dialog').contains('Cancel').click();
    gcy('project-transfer-dialog').should('not.exist');
  });

  it('transfers to organization', () => {
    login('test_username', 'admin');
    transferProject(
      'Organization owned project',
      'Owned organization',
      'Another organization'
    );
    assertTransferred('Organization owned project', 'Another organization');
  });

  after(() => {
    projectTransferringTestData.clean();
  });
});

const openTransferDialog = (projectName: string, organization: string) => {
  enterProjectSettings(projectName, organization);
  gcy('project-settings-menu-advanced').click();
  gcy('project-settings-transfer-button')
    .should('contain', 'Transfer')
    .wait(100)
    .click();
};

const transferProject = (
  projectName: string,
  organization: string,
  newOwner: string
) => {
  openTransferDialog(projectName, organization);
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
  switchToOrganization(newOwner);
  waitForGlobalLoading();
  gcy('dashboard-projects-list-item').contains(projectName);
};
