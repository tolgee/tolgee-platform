import source from './RootRouter.tsx?raw';

// Routes placed above the survey gate are not covered by it. Three gate-placement
// bugs shipped that way, each found by a human reading this file, so the exempt
// list is pinned here: adding a route above the gate fails until it is added
// below deliberately.
const EXPECTED_UNGATED_ROUTES = [
  'RESET_PASSWORD_REQUEST',
  'RESET_PASSWORD_WITH_PARAMS',
  'SIGN_UP',
  'LOGIN',
  'ACCEPT_INVITATION',
  'SSO_MIGRATION',
  'OAUTH2_CONSENT',
  'ACCEPT_AUTH_PROVIDER_CHANGE',
  'GO_TO_CLOUD_BILLING',
  'GO_TO_SELF_HOSTED_BILLING',
  'GO_TO_PREFERRED_ORGANIZATION',
];

describe('RootRouter survey coverage', () => {
  const linksBefore = (boundary: string) =>
    [
      ...source
        .slice(0, source.indexOf(boundary))
        .matchAll(/LINKS\.([A-Z0-9_]+)\.template/g),
    ].map((match) => match[1]);

  it('mounts the survey gate exactly once', () => {
    expect(source.match(/<OnboardingSurveyGate>/g)).toHaveLength(1);
  });

  it('leaves only the auth and account callbacks outside the gate', () => {
    expect(linksBefore('<OnboardingSurveyGate>')).toEqual(
      EXPECTED_UNGATED_ROUTES
    );
  });
});
