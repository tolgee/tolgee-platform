import {
  ProjectChoice,
  chosenProjectId,
} from 'tg.component/security/oauth2/consentProjectChoice';

// The client's authorize query travels on this route's own URL; the backend re-reads every parameter from it.
export const authorizeRequestFromSearch = (search: string) => {
  const params = new URLSearchParams(search);
  const value = (name: string) => params.get(name) ?? undefined;
  return {
    clientId: params.get('client_id') ?? '',
    redirectUri: params.get('redirect_uri') ?? '',
    responseType: value('response_type'),
    scope: value('scope'),
    state: value('state'),
    codeChallenge: value('code_challenge'),
    codeChallengeMethod: value('code_challenge_method'),
    project: value('project'),
  };
};

export const consentRequest = (params: {
  state: string;
  approvedScopes: string[];
  projectChoice: ProjectChoice;
}) => ({
  state: params.state,
  scopes: params.approvedScopes,
  projectScope: projectScopeOf(params.projectChoice),
  projectId: chosenProjectId(params.projectChoice),
});

const projectScopeOf = (
  choice: ProjectChoice
): 'SINGLE_PROJECT' | 'ALL_PROJECTS' | undefined => {
  switch (choice.kind) {
    case 'one':
      return 'SINGLE_PROJECT';
    case 'all':
      return 'ALL_PROJECTS';
    case 'unset':
      return undefined;
  }
};
