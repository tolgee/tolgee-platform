/// <reference types="cypress" />

import {
  createProject,
  forceDate,
  login,
  releaseForcedDate,
  setProperty,
} from '../../common/apiCalls/common';
import {
  assertMessage,
  confirmStandard,
  gcy,
  selectInSelect,
} from '../../common/shared';
import 'cypress-file-upload';
import { administrationTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import {
  getUserListItem,
  visitAdministrationOrganizations,
  visitAdministrationUsers,
} from '../../common/administration';

describe('Administration', () => {
  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generate().then((res) => {});
    login('admin@admin.com');
  });

  afterEach(() => {
    administrationTestData.clean();
    setProperty('authentication.userCanCreateOrganizations', true);
  });

  it('gets to the administration via menu', () => {
    visit();
    gcy('global-user-menu-button').click();
    gcy('user-menu-server-administration').click();
    gcy('settings-menu-item').contains('Organizations').should('be.visible');
  });

  it('there is no administration button for standard user', () => {
    login('user@user.com');
    visit();
    gcy('global-user-menu-button').click();
    gcy('user-menu-server-administration').should('not.exist');
  });

  it('can access organization projects', () => {
    visitAdministrationOrganizations();
    getOrganizationListItem()
      .findDcy('administration-organizations-projects-button')
      .click();
    assertAdminFrameVisible();
    gcy('navigation-item').contains('Projects');
  });

  it('can access organization settings', () => {
    visitAdministrationOrganizations();
    getOrganizationListItem()
      .findDcy('administration-organizations-settings-button')
      .click();
    assertAdminFrameVisible();
    cy.contains('Organization profile').should('be.visible');
  });

  it('can change user permission', () => {
    visitAdministrationOrganizations();
    gcy('settings-menu-item').contains('Users').click();
    changeUserRole('John User', 'Admin');
    changeUserRole('John User', 'Supporter');
    changeUserRole('John User', 'User');
    getUserRoleSelect('Peter Administrator')
      .find('div')
      .should('have.attr', 'aria-disabled', 'true');
  });

  it("can display user's last activity", () => {
    visitAdministrationUsers();
    getUserListItem('Peter Administrator')
      .findDcy('administration-user-activity')
      .contains('No activity yet');
    forceDate(new Date('2023-08-28').getTime());
    createProject({
      name: 'just.to.record.activity',
      languages: [{ name: 'čj', originalName: 'cs', tag: 'cs' }],
    });
    gcy('settings-menu-item').contains('Users').click(); // reload
    getUserListItem('Peter Administrator')
      .findDcy('administration-user-activity')
      .contains('Last activity on August 28, 2023 at 2:00 AM');
    releaseForcedDate();
  });

  it('can delete user', () => {
    visitAdministrationUsers();
    getUserListItem('John User').findDcy('administration-user-menu').click();
    cy.gcy('administration-user-delete-user').click();
    confirmStandard();
    assertMessage('User deleted');
    cy.contains('John User').should('not.exist');
  });

  it('only admin can add organization when userCanCreateOrganizations = false', () => {
    login('user@user.com');
    setProperty('authentication.userCanCreateOrganizations', false);
    visit();
    assertOrganizationAddButtonNotVisible();
    login('admin@admin.com');
    visit();
    assertOrganizationAddButtonVisible();
    setProperty('authentication.userCanCreateOrganizations', true);
    login('user@user.com');
    visit();
    assertOrganizationAddButtonVisible();
  });
});

const visit = () => {
  cy.visit(HOST);
};

function getOrganizationListItem() {
  return cy
    .contains('John User')
    .closestDcy('administration-organizations-list-item')
    .parent();
}

function assertAdminFrameVisible() {
  gcy('administration-access-message').should('be.visible');
  gcy('administration-frame').should('exist');
}

function getUserRoleSelect(user: string) {
  return getUserListItem(user).findDcy('administration-user-role-select');
}

function changeUserRole(user: string, role: 'Admin' | 'Supporter' | 'User') {
  selectInSelect(getUserRoleSelect(user), role);
  confirmStandard();
  assertMessage('Role changed');
}

function assertOrganizationAddButtonVisible() {
  gcy('organization-switch').click();
  gcy('organization-switch-new').should('be.visible');
}

function assertOrganizationAddButtonNotVisible() {
  gcy('organization-switch').click();
  gcy('organization-switch-new').should('not.exist');
}
