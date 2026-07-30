import { assertMessage } from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import {
  getProjectByNameFromTestData,
  membersCommunityData,
} from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { E2ProjectMembersView } from '../../compounds/projectMembers/E2ProjectMembersView';

const membersView = new E2ProjectMembersView();

const visitMembersOf = (projectName: string, body: any) => {
  const project = getProjectByNameFromTestData(body, projectName);
  login('admin@contributors.com');
  membersView.visit(project.id);
  waitForGlobalLoading();
  return project;
};

describe('Community tab on the Members page', () => {
  beforeEach(() => {
    membersCommunityData.clean();
  });

  afterEach(() => {
    membersCommunityData.clean();
  });

  it('shows Team and Community tabs on a public project and lists non-member contributors', () => {
    membersCommunityData.generateStandard().then((res) => {
      visitMembersOf('Contributors public project', res.body);

      membersView.getTeamTab().should('be.visible');
      membersView.getCommunityTab().should('be.visible');
      membersView.openCommunityTab();

      membersView.getContributors().should('have.length', 2);
      membersView.getContributor('Cora Contributor').should('exist');
      membersView.getContributor('Cody Contributor').should('exist');

      membersView.getMembers().should('not.be.visible');

      membersView
        .getContributorFirstContributions()
        .first()
        .should('be.visible')
        .and('contain', '2019');
      membersView
        .getContributorLastContributions()
        .first()
        .should('be.visible')
        .and('contain', '2021');

      membersView
        .getContributor('Cora Contributor')
        .should('contain', 'contributor@contributors.com');
      membersView
        .getContributor('Cody Contributor')
        .should('contain', 'contributor2@contributors.com');

      membersView.openTeamTab();
      membersView.getMembers().should('be.visible');
      membersView.getContributors().should('not.exist');
    });
  });

  it('invites a contributor as a member from the Community tab', () => {
    membersCommunityData.generateStandard().then((res) => {
      visitMembersOf('Contributors public project', res.body);

      membersView.openCommunityTab();
      const dialog = membersView.inviteContributor('Cora Contributor');

      dialog
        .getEmailField()
        .find('input')
        .should('have.value', 'contributor@contributors.com');
      dialog.clickInvite();
      assertMessage('Invitation was sent');
      waitForGlobalLoading();

      membersView
        .getContributorInvitationPending('Cora Contributor')
        .should('be.visible');
    });
  });

  it('offers the invite button again after the invitation is cancelled', () => {
    membersCommunityData.generateStandard().then((res) => {
      visitMembersOf('Contributors public project', res.body);

      membersView.openCommunityTab();
      membersView.inviteContributor('Cora Contributor').clickInvite();
      assertMessage('Invitation was sent');
      waitForGlobalLoading();

      membersView
        .getContributorInvitationPending('Cora Contributor')
        .should('be.visible');

      membersView.openTeamTab();
      membersView.cancelInvitation();
      waitForGlobalLoading();

      membersView.openCommunityTab();
      membersView
        .getContributorInviteButton('Cora Contributor')
        .should('be.visible');
    });
  });

  it('does not offer the invite affordance to a members-view-only user', () => {
    membersCommunityData.generateStandard().then((res) => {
      const project = getProjectByNameFromTestData(
        res.body,
        'Contributors public project'
      );
      login('viewer@contributors.com');
      membersView.visit(project.id);
      waitForGlobalLoading();

      membersView.openCommunityTab();
      membersView.getContributors().should('have.length', 2);
      membersView
        .getContributor('Cora Contributor')
        .should('contain', 'contributor@contributors.com');
      membersView.getContributorInviteButtons().should('not.exist');
      membersView.getContributorInvitationPendings().should('not.exist');
    });
  });

  it('hides the tabs on a public project with no contributors and shows the member list', () => {
    membersCommunityData.generateStandard().then((res) => {
      visitMembersOf('Contributors public empty project', res.body);

      membersView.getMembers().should('be.visible');
      membersView.getTeamTab().should('not.exist');
      membersView.getCommunityTab().should('not.exist');
    });
  });

  it('hides the tabs on a private project and shows the member list', () => {
    membersCommunityData.generateStandard().then((res) => {
      visitMembersOf('Contributors project', res.body);

      membersView.getMembers().should('be.visible');
      membersView.getTeamTab().should('not.exist');
      membersView.getCommunityTab().should('not.exist');
    });
  });
});
