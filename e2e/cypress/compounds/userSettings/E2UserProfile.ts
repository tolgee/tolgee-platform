export class E2UserProfile {
  getVisibilitySwitch() {
    return cy.gcy('user-profile-visibility-switch');
  }

  getVisibilitySwitchInput() {
    return this.getVisibilitySwitch().find('input');
  }
}
