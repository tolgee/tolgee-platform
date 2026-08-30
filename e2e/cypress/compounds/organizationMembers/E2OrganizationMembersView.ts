import { confirmStandard, gcyAdvanced } from '../../common/shared';

export class E2OrganizationMembersView {
  getMember(username: string) {
    return gcyAdvanced({ value: 'organization-member-item', username });
  }

  disableMember(username: string) {
    this.getMember(username)
      .findDcy('organization-members-disable-user-button')
      .click();
    confirmStandard();
  }

  enableMember(username: string) {
    this.getMember(username)
      .findDcy('organization-members-enable-user-button')
      .click();
    confirmStandard();
  }
}
