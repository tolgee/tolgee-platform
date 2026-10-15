import { visitAdministration } from '../../common/administration';
import { gcy, gcyAdvanced } from '../../common/shared';

export class E2AdministrationOrganizations {
  visit() {
    visitAdministration();
  }

  openFeaturesDialog(organizationSlug: string) {
    this.visit();
    gcyAdvanced({
      value: 'administration-organizations-features-button',
      slug: organizationSlug,
    }).click();
    return this.getFeaturesDialog().should('be.visible');
  }

  getFeaturesDialog() {
    return gcy('administration-organization-features-dialog');
  }

  getFeatureFlagCheckbox(flag: string) {
    return gcyAdvanced({
      value: 'administration-organization-feature-flag-checkbox',
      flag,
    }).find('input');
  }

  getPlanFeatureCheckbox(feature: string) {
    return gcyAdvanced({
      value: 'administration-organization-plan-feature-checkbox',
      feature,
    }).find('input');
  }

  saveFeatures() {
    gcy('administration-organization-features-save').click();
    this.getFeaturesDialog().should('not.exist');
  }
}
