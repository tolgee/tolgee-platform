import 'cypress-file-upload';
import {
  confirmStandard,
  gcy,
  goToPage,
  selectInProjectMenu,
} from '../../common/shared';
import {
  enterProject,
  enterProjectSettings,
  visitList,
} from '../../common/projects';
import { waitForGlobalLoading } from '../../common/loading';

import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { permissionsMenuSelectRole } from '../../common/permissionsMenu';
import { checkNumberOfMenuItems } from '../../common/permissions/main';

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

    describe('Modifying', () => {
      beforeEach(() => {
        projectTestData.clean();
        projectTestData.generate();
        login('cukrberg@facebook.com', 'admin');
      });

      it('Can modify permissions', () => {
        visitList();
        enterProjectSettings('Facebook itself');
        selectInProjectMenu('Members');
        gcy('global-paginated-list').within(() => {
          gcy('project-member-item')
            .contains('Vaclav Novak')
            .closestDcy('project-member-item')
            .within(() => {
              gcy('permissions-menu-button')
                .should('be.visible')
                // clicks the button even if detached from dom
                .click({ force: true });
            });
        });
        permissionsMenuSelectRole('Translate');
        login('vaclav.novak@fake.com', 'admin');
        visitList();
        enterProject('Facebook itself', 'Facebook');
        checkNumberOfMenuItems(6);
      });

      it('Can revoke permissions', () => {
        visitList();
        enterProjectSettings('Facebook itself');
        selectInProjectMenu('Members');

        gcy('global-paginated-list').within(() => {
          gcy('project-member-item')
            .contains('Vaclav Novak')
            .closestDcy('project-member-item')
            .within(() => {
              gcy('project-member-revoke-button')
                .should('be.visible')
                // clicks the button even if detached from dom
                .click({ force: true });
            });
        });
        confirmStandard();
        login('vaclav.novak@fake.com', 'admin');
        visitList();
        cy.contains('Facebook itself').should('not.exist');
      });
    });
  });
});
