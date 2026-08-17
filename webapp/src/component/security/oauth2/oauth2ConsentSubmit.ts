const API_URL = import.meta.env.VITE_APP_API_URL || '';

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
