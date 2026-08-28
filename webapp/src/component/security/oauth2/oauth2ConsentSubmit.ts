import { apiV2HttpService } from 'tg.service/http/ApiV2HttpService';

const API_URL = import.meta.env.VITE_APP_API_URL || '';

export const ALL_PROJECTS = 'all' as const;

// Must run before submitConsentForm: the chosen project has to be on the authorization by the time the code is issued.
export const selectConsentProject = (params: {
  state: string;
  selectedProject: number | typeof ALL_PROJECTS;
}) => {
  const projectQuery =
    params.selectedProject === ALL_PROJECTS
      ? ''
      : `&projectId=${params.selectedProject}`;
  // useApiMutation can't type this call: select-project has no request body, and RequestParamsType intersects
  // parameters & requestBody, so the mutation's variables collapse to `never` (a query-only POST doesn't fit).
  return apiV2HttpService.post(
    `oauth2/select-project?state=${encodeURIComponent(
      params.state
    )}${projectQuery}`,
    {}
  );
};

// Real form POST (not fetch) so the browser sends the session cookie and follows the redirect back to the client.
export const submitConsentForm = (params: {
  clientId: string;
  state: string;
  approvedScopes: string[];
}) => {
  const form = document.createElement('form');
  form.method = 'post';
  form.action = `${API_URL}/oauth2/authorize`;
  const addField = (name: string, value: string) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  };
  addField('client_id', params.clientId);
  addField('state', params.state);
  params.approvedScopes.forEach((s) => addField('scope', s));
  document.body.appendChild(form);
  form.submit();
};
