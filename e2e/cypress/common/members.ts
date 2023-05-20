export function getMemberRow(user: string) {
  return cy
    .gcy('project-member-item')
    .contains(user)
    .closestDcy('project-member-item');
}

export function revokeMemberAccess(user: string) {
  return getMemberRow(user).within(() =>
    cy
      .gcy('project-member-revoke-button')
      .should('be.visible')
      // clicks the button even if detached from dom
      .click({ force: true })
  );
}

export function openMemberSettings(user: string) {
  return getMemberRow(user).within(() =>
    cy
      .gcy('permissions-menu-button')
      .should('be.visible')
      // clicks the button even if detached from dom
      .click({ force: true })
  );
}
