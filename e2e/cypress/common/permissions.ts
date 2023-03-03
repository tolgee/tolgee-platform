import { components } from '../../../webapp/src/service/apiSchema.generated';
import { waitForGlobalLoading } from './loading';
import { editCell } from './translations';

export type ComputedPermissionModel =
  components['schemas']['ComputedPermissionModel'];

export function checkNumberOfMenuItems(count: number) {
  cy.gcy('project-menu-items')
    .findDcy('project-menu-item')
    .should('have.length', count);
}

type DeepOr<T, U> = {
  [K in keyof T | keyof U]: K extends keyof T
    ? K extends keyof U
      ? T[K] | U[K]
      : T[K]
    : K extends keyof U
    ? U[K]
    : never;
};

type MenuItem = Exclude<
  DataCy.Value & `project-menu-item-${string}`,
  'project-menu-item-projects'
>;

type AdditionalValues = {
  'project-menu-item-dashboard': 'test';
};

type Settings = Partial<DeepOr<AdditionalValues, Record<MenuItem, true>>>;

function tryClickIfClickable(item: DataCy.Value) {
  cy.gcy(item).then(($el) => {
    if ($el.hasClass('clickable')) {
      cy.gcy(item).click();
      waitForGlobalLoading();
      cy.go('back');
      waitForGlobalLoading();
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

function testTranslations(permissions: ComputedPermissionModel) {
  cy.gcy('translations-table-cell').contains('key-1').should('be.visible');

  if (permissions.scopes.includes('keys.edit')) {
    editCell('key-1', 'new-key');
  }

  if (permissions.scopes.includes('screenshots.view')) {
    cy.gcy('translations-table-cell').first().focus();
    cy.gcy('translations-cell-screenshots-button').should('exist');
  }
}

export function checkItemsInMenu(
  permissions: ComputedPermissionModel,
  settings: Settings
) {
  checkNumberOfMenuItems(Object.keys(settings).length + 1);
  Object.keys(settings).forEach((item: MenuItem) => {
    cy.gcy(item).click();
    waitForGlobalLoading();
    // const values = (Array.isArray(settings[item]) ? settings[item] : []) as any;
    switch (item as keyof MenuItem) {
      case 'project-menu-item-dashboard':
        testDashboard(permissions);
        break;
      case 'project-menu-item-translations':
        testTranslations(permissions);
        break;
    }
  });
}
