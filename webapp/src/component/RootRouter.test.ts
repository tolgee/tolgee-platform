import source from './RootRouter.tsx?raw';

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

const GATE = '<OnboardingSurveyGate>';

describe('RootRouter survey coverage', () => {
  const linksBefore = (boundary: string) =>
    [
      ...source
        .slice(0, source.indexOf(boundary))
        .matchAll(/LINKS\.([A-Z0-9_]+)\.template/g),
    ].map((match) => match[1]);

  it('mounts the survey gate exactly once', () => {
    expect(source.match(new RegExp(GATE, 'g'))).toHaveLength(1);
  });

  it('leaves only the auth and account callbacks outside the gate', () => {
    expect(linksBefore(GATE)).toEqual(EXPECTED_UNGATED_ROUTES);
  });

  // linksBefore only sees LINKS.X.template, so count the route elements too: a
  // route added above the gate with a literal path would be invisible otherwise.
  // The extra one is the pathless <Route> that wraps the gate.
  it('has no route above the gate that this file cannot name', () => {
    const routesAbove = source
      .slice(0, source.indexOf(GATE))
      .match(/<(?:Private|PublicOnly)?Route[\s>]/g);
    expect(routesAbove).toHaveLength(EXPECTED_UNGATED_ROUTES.length + 1);
  });
});
