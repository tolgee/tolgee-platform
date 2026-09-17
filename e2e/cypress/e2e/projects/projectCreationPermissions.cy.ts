import { HOST } from '../../common/constants';
import {
  login,
  setOrganizationCreationAllowed,
} from '../../common/apiCalls/common';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import {
  assertSwitchedToOrganization,
  gcy,
  openPublicProject,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('Project creation permissions', () => {
  beforeEach(() => {
    setOrganizationCreationAllowed(false);
    publicProjectsData.clean();
    publicProjectsData.generateStandard();
  });

  afterEach(() => {
    publicProjectsData.clean();
    setOrganizationCreationAllowed(true);
  });

  it('offers the add-project button to a server admin who is a member', () => {
    login('adminMemberUser');
    cy.visit(`${HOST}/`);
    waitForGlobalLoading();
    assertSwitchedToOrganization('publicProjectsUser');
    gcy('global-plus-button').should('exist');
  });

  it('offers no add-project button to a supporter who is not a member', () => {
    login('supporterCommunityUser');
    openPublicProject('Community Alpha');
    assertSwitchedToOrganization('publicProjectsUser');

    cy.visit(`${HOST}/`);
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('exist');
    gcy('global-plus-button').should('not.exist');
  });
});
