import { components } from '../../../webapp/src/service/apiSchema.generated';
import { login, v2apiFetch } from './apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from './apiCalls/testData/testData';
import { commentsButton, deleteComment, resolveComment } from './comments';
import { HOST } from './constants';
import { waitForGlobalLoading } from './loading';
import { confirmStandard } from './shared';
import { editCell } from './translations';

export const SKIP = false;
export const RUN = true;

export type ComputedPermissionModel =
  components['schemas']['ComputedPermissionModel'];

export function checkNumberOfMenuItems(count: number) {
  cy.gcy('project-menu-items')
    .findDcy('project-menu-item')
    .should('have.length', count);
}

type MenuItem = Exclude<
  DataCy.Value & `project-menu-item-${string}`,
  'project-menu-item-projects'
>;

type Settings = Partial<Record<MenuItem, boolean>>;

const pageIsPermitted = () => {
  waitForGlobalLoading();
  cy.get('.SnackbarItem-variantError', { timeout: 0 }).should('not.exist');
};

function tryClickIfClickable(item: DataCy.Value) {
  cy.gcy(item).then(($el) => {
    if ($el.hasClass('clickable')) {
      cy.gcy(item).click();
      pageIsPermitted();
      cy.go('back');
      pageIsPermitted();
    }
  });
}

function testDashboard(permissions: ComputedPermissionModel) {
  tryClickIfClickable('project-dashboard-language-count');
  tryClickIfClickable('project-dashboard-text');
  tryClickIfClickable('project-dashboard-progress');
  tryClickIfClickable('project-dashboard-members');
  if (permissions.scopes.includes('activity.view')) {
    cy.gcy('project-dashboard-activity-list').should('be.visible');
    cy.gcy('project-dashboard-activity-chart').should('be.visible');
  }
}

function testTranslations({ scopes }: ComputedPermissionModel) {
  cy.gcy('translations-table-cell').contains('key-1').should('be.visible');

  if (scopes.includes('keys.edit')) {
    editCell('key-1', 'new-key');
  }

  if (scopes.includes('screenshots.view')) {
    cy.gcy('translations-table-cell').first().focus();
    cy.gcy('translations-cell-screenshots-button').should('exist').click();
    cy.gcy('screenshot-thumbnail').should('be.visible');
    if (scopes.includes('screenshots.delete')) {
      cy.gcy('screenshot-thumbnail').trigger('mouseover');
      cy.gcy('screenshot-thumbnail-delete').click();
      confirmStandard();
      waitForGlobalLoading();
      cy.gcy('screenshot-thumbnail').should('not.exist');
    }
    if (scopes.includes('screenshots.upload')) {
      cy.gcy('add-box').should('be.visible');
    }
    // close popup
    cy.get('body').click(0, 0);
  }

  if (scopes.includes('translations.view')) {
    cy.gcy('translations-state-indicator').should('be.visible');

    commentsButton(0, 'de').click();
    cy.gcy('comment-text').should('be.visible');

    if (scopes.includes('translation-comments.set-state')) {
      resolveComment('comment 1');
    }

    if (scopes.includes('translation-comments.edit')) {
      deleteComment('comment 1');
    }

    if (scopes.includes('translation-comments.add')) {
      cy.gcy('translations-comments-input')
        .type('test comment')
        .type('{enter}');
      waitForGlobalLoading();
      cy.gcy('comment-text').contains('test comment').should('be.visible');
    }

    cy.gcy('translations-cell-close').click();
  } else {
    // no translations shoud be visible when user has no access to them
    cy.gcy('translations-state-indicator').should('not.exist');
  }

  if (scopes.includes('translations.edit')) {
    cy.gcy('translations-table-cell-translation').eq(1).click();
    cy.gcy('global-editor').should('be.visible');
    cy.gcy('translation-tools-machine-translation-item').should('be.visible');
    cy.gcy('translation-tools-translation-memory-item').should('be.visible');
    cy.gcy('translations-cell-close').click();
  }

  if (
    !scopes.includes('translations.edit') &&
    scopes.includes('translations.view')
  ) {
    cy.gcy('translations-table-cell-translation').first().click();
    cy.gcy('global-editor').should('not.exist');
  }
}

export function checkItemsInMenu(
  permissions: ComputedPermissionModel,
  settings: Settings
) {
  checkNumberOfMenuItems(Object.keys(settings).length + 1);
  Object.keys(settings).forEach((item: MenuItem) => {
    const value = settings[item];

    if (value !== SKIP) {
      // go to page
      cy.gcy(item).click();
      // check if there an error
      pageIsPermitted();

      switch (item as keyof MenuItem) {
        case 'project-menu-item-dashboard':
          testDashboard(permissions);
          break;
        case 'project-menu-item-translations':
          testTranslations(permissions);
          break;
      }
    }
  });
}

export function visitProjectWithPermissions(
  options: Partial<PermissionsOptions>
) {
  let projectId: number;
  let permissions: ComputedPermissionModel;
  return generatePermissionsData
    .clean()
    .then(() => generatePermissionsData.generate(options))
    .then((res) => {
      projectId = res.body.projects[0].id;
    })
    .then(() => login('me@me.me'))
    .then(() => v2apiFetch(`projects/${projectId}`))
    .then((res) => {
      permissions = res.body.computedPermission;
    })
    .then(() => cy.visit(`${HOST}/projects/${projectId}`))
    .then(() => ({ projectId, permissions }));
}
