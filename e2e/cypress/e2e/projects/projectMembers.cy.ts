import 'cypress-file-upload';
import {
  confirmStandard,
  gcy,
  goToPage,
  selectInProjectMenu,
  visitProjectDashboard,
  visitProjectMembers,
} from '../../common/shared';
import { enterProjectSettings, visitList } from '../../common/projects';
import { waitForGlobalLoading } from '../../common/loading';

import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { permissionsMenuSelectRole } from '../../common/permissionsMenu';
import {
  checkItemsInMenu,
  loginAndGetInfo,
  RUN,
  visitProjectWithPermissions,
} from '../../common/permissions/main';
import { ProjectInfo } from '../../common/permissions/shared';
import { openMemberSettings, revokeMemberAccess } from '../../common/members';

describe('Project members', () => {
  beforeEach(() => {});

  afterEach(() => {
    waitForGlobalLoading();
  });

  describe('Permission settings', () => {
    describe('Not modifying', () => {
      before(() => {
        projectTestData.clean();
        projectTestData.generate();
      });

      beforeEach(() => {
        login('cukrberg@facebook.com', 'admin');
      });

      it('Can search in permissions', () => {
        visitList();
        enterProjectSettings('Facebook itself', 'Facebook');
        selectInProjectMenu('Members');
        gcy('global-list-search').find('input').type('Doe');
        gcy('global-paginated-list').within(() => {
          gcy('project-member-item')
            .should('have.length', 1)
            .should('contain', 'John Doe');
        });
      });

      it('Can paginate', () => {
        visitList();
        login('gates@microsoft.com', 'admin');
        enterProjectSettings('Microsoft Word', 'Microsoft');
        selectInProjectMenu('Members');
        goToPage(2);
        cy.contains('owner@zzzcool9.com (owner@zzzcool9.com)').should(
          'be.visible'
        );
      });

      it('Has enabled proper options for each user', () => {
        visitList();
        enterProjectSettings('Facebook itself', 'Facebook');
        selectInProjectMenu('Members');
        gcy('global-paginated-list').within(() => {
          gcy('project-member-item')
            .contains('John Doe')
            .closestDcy('project-member-item')
            .within(() => {
              gcy('project-member-revoke-button').should('be.disabled');
              gcy('permissions-menu-button').should('be.enabled');
            });
          gcy('project-member-item')
            .contains('Cukrberg')
            .closestDcy('project-member-item')
            .within(() => {
              gcy('project-member-revoke-button').should('be.disabled');
              gcy('permissions-menu-button').should('be.disabled');
            });
        });
      });
    });

    describe('Revoking', () => {
      beforeEach(() => {
        projectTestData.clean();
        projectTestData.generate();
        login('cukrberg@facebook.com', 'admin');
      });

      it('Can revoke permissions', () => {
        visitList();
        enterProjectSettings('Facebook itself');
        selectInProjectMenu('Members');

        revokeMemberAccess('Vaclav Novak');
        confirmStandard();
        login('vaclav.novak@fake.com', 'admin');
        visitList();
        cy.contains('Facebook itself').should('not.exist');
      });
    });
  });

  describe('Modifying access', () => {
    let info: ProjectInfo;
    beforeEach(async () => {
      info = await visitProjectWithPermissions(
        {
          scopes: ['activity.view'],
        },
        'admin@admin.com'
      );
    });

    it('selects Translate role for the user', () => {
      visitProjectMembers(info.project.id);
      openMemberSettings('me@me.me');
      permissionsMenuSelectRole('Translate', { languages: ['Czech'] });

      loginAndGetInfo('me@me.me', info.project.id).then((info) => {
        expect(info.project.computedPermission.viewLanguageIds.length).equal(3);
        expect(
          info.project.computedPermission.translateLanguageIds.length
        ).equal(1);
        visitProjectDashboard(info.project.id);
        checkItemsInMenu(info, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-import': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      });
    });

    it('selects Review role for the user', () => {
      visitProjectMembers(info.project.id);
      openMemberSettings('me@me.me');
      permissionsMenuSelectRole('Review', { languages: ['Czech'] });

      loginAndGetInfo('me@me.me', info.project.id).then((info) => {
        expect(info.project.computedPermission.viewLanguageIds.length).equal(3);
        expect(
          info.project.computedPermission.translateLanguageIds.length
        ).equal(1);
        expect(
          info.project.computedPermission.stateChangeLanguageIds.length
        ).equal(1);
        visitProjectDashboard(info.project.id);
        checkItemsInMenu(info, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-import': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      });
    });
  });
});
