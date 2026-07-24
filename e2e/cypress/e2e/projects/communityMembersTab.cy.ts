import { gcy, gcyAdvanced, visitProjectMembers } from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import {
  getProjectByNameFromTestData,
  membersCommunityData,
} from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';

describe('Community tab on the Members page', () => {
  beforeEach(() => {
    membersCommunityData.clean();
  });

  afterEach(() => {
    membersCommunityData.clean();
  });

  it('shows Team and Community tabs on a public project and lists non-member contributors without email', () => {
    membersCommunityData.generateStandard().then((res) => {
      const project = getProjectByNameFromTestData(
        res.body,
        'Contributors public project'
      );
      login('admin@contributors.com');
      visitProjectMembers(project.id);
      waitForGlobalLoading();

      gcy('project-members-tab-team').should('be.visible');
      gcy('project-members-tab-community').should('be.visible').click();

      gcy('project-contributor-item').should('have.length', 2);
      gcyAdvanced({
        value: 'project-contributor-item',
        name: 'Cora Contributor',
      }).should('exist');
      gcyAdvanced({
        value: 'project-contributor-item',
        name: 'Cody Contributor',
      }).should('exist');

      gcy('project-member-item').should('not.be.visible');

      // Dates are seeded 2019 (first) and 2021 (last) by MembersCommunityE2eDataController.
      gcy('project-contributor-item-first-contribution')
        .first()
        .should('be.visible')
        .and('contain', '2019');
      gcy('project-contributor-item-last-contribution')
        .first()
        .should('be.visible')
        .and('contain', '2021');

      cy.contains('contributor@contributors.com').should('not.exist');
      cy.contains('contributor2@contributors.com').should('not.exist');

      gcy('project-members-tab-team').click();
      gcy('project-member-item').should('be.visible');
      gcy('project-contributor-item').should('not.exist');
    });
  });

  it('hides the tabs on a public project with no contributors and shows the member list', () => {
    membersCommunityData.generateStandard().then((res) => {
      const project = getProjectByNameFromTestData(
        res.body,
        'Contributors public empty project'
      );
      login('admin@contributors.com');
      visitProjectMembers(project.id);
      waitForGlobalLoading();

      gcy('project-member-item').should('be.visible');
      gcy('project-members-tab-team').should('not.exist');
      gcy('project-members-tab-community').should('not.exist');
    });
  });

  it('hides the tabs on a private project and shows the member list', () => {
    membersCommunityData.generateStandard().then((res) => {
      const project = getProjectByNameFromTestData(
        res.body,
        'Contributors project'
      );
      login('admin@contributors.com');
      visitProjectMembers(project.id);
      waitForGlobalLoading();

      gcy('project-member-item').should('be.visible');
      gcy('project-members-tab-team').should('not.exist');
      gcy('project-members-tab-community').should('not.exist');
    });
  });
});
