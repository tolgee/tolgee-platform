import {
  authorizeRequestFromSearch,
  consentRequest,
} from 'tg.component/security/oauth2/oauth2ConsentSubmit';

describe('authorizeRequestFromSearch', () => {
  it('carries every authorize parameter through', () => {
    const request = authorizeRequestFromSearch(
      '?client_id=ext&redirect_uri=https%3A%2F%2Fa.test%2Fcb&response_type=code' +
        '&scope=keys.view%20translations.view&state=s1&code_challenge=c&code_challenge_method=S256&project=7'
    );
    expect(request).toEqual({
      clientId: 'ext',
      redirectUri: 'https://a.test/cb',
      responseType: 'code',
      scope: 'keys.view translations.view',
      state: 's1',
      codeChallenge: 'c',
      codeChallengeMethod: 'S256',
      project: '7',
    });
  });

  it('leaves absent optional parameters undefined rather than empty', () => {
    const request = authorizeRequestFromSearch('?client_id=ext&redirect_uri=x');
    expect(request.state).toBeUndefined();
    expect(request.scope).toBeUndefined();
    expect(request.project).toBeUndefined();
  });
});

describe('consentRequest', () => {
  it('sends the approved scopes and the chosen project', () => {
    expect(
      consentRequest({
        state: 'the-state',
        approvedScopes: ['keys.view', 'translations.view'],
        projectChoice: { kind: 'one' as const, project: { id: 42, name: 'P' } },
      })
    ).toEqual({
      state: 'the-state',
      scopes: ['keys.view', 'translations.view'],
      projectScope: 'SINGLE_PROJECT',
      projectId: 42,
    });
  });

  it('says ALL_PROJECTS explicitly rather than leaving projectId absent', () => {
    const request = consentRequest({
      state: 's',
      approvedScopes: ['keys.view'],
      projectChoice: { kind: 'all' as const },
    });
    expect(request.projectScope).toBe('ALL_PROJECTS');
    expect(request.projectId).toBeUndefined();
  });

  it('reports the real choice on a denial rather than the widest one', () => {
    const request = consentRequest({
      state: 's',
      approvedScopes: [],
      projectChoice: { kind: 'one' as const, project: { id: 42, name: 'P' } },
    });
    expect(request.scopes).toEqual([]);
    expect(request.projectScope).toBe('SINGLE_PROJECT');
  });
});
