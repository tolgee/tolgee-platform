import { confirmStandard, gcyAdvanced } from '../../common/shared';

export class E2OrganizationMembersView {
  getMember(username: string) {
    return gcyAdvanced({ value: 'organization-member-item', username });
  }

  getRemoveButton(username: string) {
    return this.getMember(username).findDcy(
      'organization-members-remove-user-button'
    );
  }

  getDisableButton(username: string) {
    return this.getMember(username).findDcy(
      'organization-members-disable-user-button'
    );
  }

  getEnableButton(username: string) {
    return this.getMember(username).findDcy(
      'organization-members-enable-user-button'
    );
  }

  getDisabledLabel(username: string) {
    return this.getMember(username).findDcy(
      'organization-member-disabled-label'
    );
  }

  disableMember(username: string) {
    this.getDisableButton(username).click();
    confirmStandard();
  }

  enableMember(username: string) {
    this.getEnableButton(username).click();
    confirmStandard();
  }
}
