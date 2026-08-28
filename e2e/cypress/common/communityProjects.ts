import { HOST } from './constants';
import { publicProjectsData } from './apiCalls/testData/testData';
import { gcyAdvanced } from './shared';
import { waitForGlobalLoading } from './loading';
import { setOrganizationCreationAllowed } from './apiCalls/common';

export const PUBLIC_PROJECT_NAME = 'Community Alpha';

export const SET_PREFERRED_ORG =
  '**/v2/user-preferences/set-preferred-organization/*';

const COMMUNITY_ORGANIZATION_NAMES = [
  'publicProjectsUser',
  'Community User',
  'Dual Org Member',
  'Members Only Outfit',
] as const;

type CommunityOrganizationName = typeof COMMUNITY_ORGANIZATION_NAMES[number];

export type CommunityProjectsFixture = {
  organizations: Record<
    CommunityOrganizationName,
    { slug: string; id: number }
  >;
  privateProjectId: number;
  publicProjectId: number;
};

export const communityProjectsFixture = (): CommunityProjectsFixture => {
  const emptyOrganizations = () =>
    ({} as CommunityProjectsFixture['organizations']);

  const fixture: CommunityProjectsFixture = {
    organizations: emptyOrganizations(),
    privateProjectId: 0,
    publicProjectId: 0,
  };

  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generateStandard().then((res) => {
      const organizations = emptyOrganizations();
      res.body.organizations.forEach((org) => {
        organizations[org.name as CommunityOrganizationName] = org;
      });
      COMMUNITY_ORGANIZATION_NAMES.forEach((name) => {
        if (!organizations[name]) {
          throw new Error(
            `PublicProjectsE2eData no longer creates an organization named "${name}"`
          );
        }
      });
      fixture.organizations = organizations;
      fixture.privateProjectId = res.body.projects.find(
        (project) => project.name === 'Private project'
      ).id;
      fixture.publicProjectId = res.body.projects.find(
        (project) => project.name === PUBLIC_PROJECT_NAME
      ).id;
    });
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  return fixture;
};

const DEFAULT_HOLD_MS = 2000;
export const HELD_REQUEST_MS = 6000;

/** Mirrors ORGANIZATION_SWITCH_DEADLINE_MS in webapp/src/globalContext/organizationSwitchSequencer.ts. */
export const SWITCH_DEADLINE_MS = 8000;

/** Long enough that the switch deadline fires while the write is still held. */
export const PAST_SWITCH_DEADLINE_MS = SWITCH_DEADLINE_MS + 4000;

/**
 * Register before any retrying assertion in an `afterEach`: `userCanCreateOrganizations` is
 * server-wide, and a hook that times out aborts the ones registered after it.
 */
export const restoreOrganizationCreation = () => {
  afterEach(() => {
    setOrganizationCreationAllowed(true);
  });
};

export const keepPersonasOrganizationLess = () => {
  restoreOrganizationCreation();

  beforeEach(() => {
    setOrganizationCreationAllowed(false);
  });
};

export const interceptPreferredOrgDelayed = (
  alias: string,
  delayMs: number = DEFAULT_HOLD_MS,
  onlyForId?: () => number
) =>
  cy
    .intercept('PUT', SET_PREFERRED_ORG, (req) =>
      req.continue((res) => {
        if (!onlyForId || req.url.endsWith(`/${onlyForId()}`)) {
          res.setDelay(delayMs);
        }
      })
    )
    .as(alias);

export const interceptPreferredOrgForbidden = () =>
  cy.intercept('PUT', SET_PREFERRED_ORG, {
    statusCode: 403,
    body: { code: 'operation_not_permitted' },
  });

/** Enters through /community-projects — the surface this feature builds, not the anonymous list. */
export const openPublicProject = () => {
  cy.visit(`${HOST}/community-projects`);
  waitForGlobalLoading();
  gcyAdvanced({
    value: 'dashboard-projects-list-item',
    name: PUBLIC_PROJECT_NAME,
  }).click();
  cy.url().should('match', /\/projects\/[0-9]+/);
  waitForGlobalLoading();
};
