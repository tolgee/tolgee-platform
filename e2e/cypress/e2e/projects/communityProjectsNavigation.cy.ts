import { HOST } from '../../common/constants';
import {
  createUser,
  deleteAllEmails,
  deleteUserSql,
  disableEmailVerification,
  enableEmailVerification,
  login,
  logout,
  setBypassSeatCountCheck,
} from '../../common/apiCalls/common';
import {
  organizationTestData,
  publicProjectsData,
} from '../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced, switchToOrganization } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('Community projects navigation', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean({ timeout: 120000 });
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visitProjects();
    });
  });

  afterEach(() => {
    organizationTestData.clean();
    setBypassSeatCountCheck(false);
  });

  const visitProjects = () => {
    cy.visit(`${HOST}/projects`);
    cy.waitForDom();
  };

  const openSwitch = () => {
    cy.waitForDom();
    gcy('organization-switch').click();
    cy.waitForDom();
  };

  const visitCommunity = () => {
    cy.visit(`${HOST}/community-projects`);
    cy.waitForDom();
  };

  // TODO: the "Community translation" switcher entry is currently disabled (it will return once
  // contributor tracking lands in a future pitch). Re-enable these five skipped tests — the ones
  // that drive `switch-popover-footer-action` — when the button is added back.
  it.skip('navigates to the community page via the dropdown entry (mouse)', () => {
    openSwitch();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).click();
    cy.location('pathname').should('eq', '/community-projects');
  });

  it.skip('navigates to the community page via the dropdown entry (keyboard)', () => {
    openSwitch();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    })
      .focus()
      .type('{enter}');
    cy.location('pathname').should('eq', '/community-projects');
  });

  it('shows the community chrome: banner, community chip, no add button', () => {
    visitCommunity();
    gcy('community-projects-view').should('be.visible');
    gcy('community-translation-banner').should('be.visible');
    gcy('organization-switch')
      .findDcy('community-translation-item')
      .should('be.visible');
    gcy('global-plus-button').should('not.exist');
  });

  it('returns to the org list when selecting an org from the community switcher', () => {
    visitCommunity();
    switchToOrganization('Microsoft');
    cy.location('pathname').should('eq', '/');
    cy.waitForDom();
    gcy('organization-switch').contains('Microsoft').should('be.visible');

    cy.reload();
    cy.waitForDom();
    gcy('organization-switch').contains('Microsoft').should('be.visible');
  });

  it('does not navigate away when selecting an org from the normal list switcher', () => {
    switchToOrganization('Microsoft');
    cy.location('pathname').should('eq', '/');
  });

  it('renders the Projects title with the inline switcher on both list pages', () => {
    gcy('global-base-view-title').contains('Projects').should('be.visible');
    gcy('global-base-view-title')
      .findDcy('organization-switch')
      .should('be.visible');

    visitCommunity();
    gcy('global-base-view-title').contains('Projects').should('be.visible');
    gcy('global-base-view-title')
      .findDcy('organization-switch')
      .should('be.visible');
  });

  it.skip('highlights no org row while on the community page but still offers the community entry', () => {
    visitCommunity();
    openSwitch();
    gcy('switch-popover-item').should('exist');
    gcy('switch-popover-item').filter('.Mui-selected').should('not.exist');
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).should('be.visible');
  });

  it.skip('offers the community entry from a switcher outside the projects pages', () => {
    cy.visit(
      `${HOST}/organizations/${organizationData['Tolgee'].slug}/members`
    );
    openSwitch();
    gcy('switch-popover-item').should('exist');
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).should('be.visible');
  });

  it('shows the empty state and hides search when there are no public projects', () => {
    publicProjectsData.clean();
    visitCommunity();
    waitForGlobalLoading();
    gcy('community-projects-view').should('be.visible');
    gcy('global-paginated-list').should('be.visible');
    gcy('dashboard-projects-list-item').should('not.exist');
    gcy('global-list-search').should('not.exist');
  });

  it.skip('closes the popover on Escape from the footer entry', () => {
    openSwitch();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    })
      .focus()
      .type('{esc}');
    gcy('switch-popover-item').should('not.exist');
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).should('not.exist');
  });
});

describe('Community projects email-verification gate', () => {
  const email = 'community-projects-unverified@doe.com';
  const password = 'verysecurepassword';
  const changedEmail = 'community-projects-changed@doe.com';

  beforeEach(() => {
    enableEmailVerification();
    deleteUserSql(email);
    deleteUserSql(changedEmail);
    deleteAllEmails();
    createUser(email, password);
    login(email, password);
    // Changing the email while verification is enabled puts the authenticated session into the
    // awaiting-verification state, which is what unverified users see.
    cy.visit(`${HOST}/account/profile`);
    cy.get('form').findInputByName('email').clear().type(changedEmail);
    cy.xpath("//*[@name='currentPassword']").clear().type(password);
    gcy('global-form-save-button').click();
    waitForGlobalLoading();
  });

  afterEach(() => {
    deleteUserSql(email);
    deleteUserSql(changedEmail);
    deleteAllEmails();
    disableEmailVerification();
  });

  it('shows EmailNotVerifiedView instead of the switcher for unverified users', () => {
    cy.visit(`${HOST}/community-projects`);
    waitForGlobalLoading();
    gcy('resend-email-button').should('be.visible');
    gcy('organization-switch').should('not.exist');
  });
});

describe('Community projects list content', () => {
  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generateStandard();
    login('publicProjectsUser');
    cy.visit(`${HOST}/community-projects`);
    waitForGlobalLoading();
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  it('lists public projects across orgs with the public badge, hiding private ones', () => {
    gcy('dashboard-projects-list-item').should('have.length', 7);
    gcy('project-list-public-badge').should('have.length', 7);
    cy.contains('Community Alpha').should('be.visible');
    cy.contains('Community Zeta').should('be.visible');
    cy.contains('Private project').should('not.exist');
  });

  it('narrows the community list with search', () => {
    gcy('global-list-search').should('exist');
    gcy('global-list-search').find('input').type('Alpha');
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('have.length', 1);
    cy.contains('Community Alpha').should('be.visible');
  });

  it('keeps the search field visible after clearing an active search', () => {
    gcy('global-list-search').find('input').type('Alpha');
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('have.length', 1);
    gcy('global-list-search').find('input').clear();
    // The list-length assertion must come first: it retries until the debounced cleared-search
    // refetch lands, so the focus check below runs only after the field has (or hasn't) remounted.
    gcy('dashboard-projects-list-item').should('have.length', 7);
    gcy('global-list-search').find('input').should('be.focused');
  });
});

describe('Community projects search threshold', () => {
  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generateFew();
    login('publicProjectsUser');
    cy.visit(`${HOST}/community-projects`);
    waitForGlobalLoading();
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  it('hides the search field at or below the project threshold', () => {
    gcy('dashboard-projects-list-item').should('have.length', 5);
    gcy('global-list-search').should('not.exist');
  });
});

describe('Community projects access', () => {
  it('redirects an unauthenticated visitor to login', () => {
    logout();
    cy.visit(`${HOST}/community-projects`);
    cy.location('pathname').should('include', '/login');
  });
});
