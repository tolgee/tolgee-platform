import { API_URL, PASSWORD, USERNAME } from '../constants';
import { ArgumentTypes, Scope } from '../types';
import { ApiKeyDTO } from '../../../../webapp/src/service/response.types';
import { components } from '../../../../webapp/src/service/apiSchema.generated';
import bcrypt = require('bcryptjs');
import Chainable = Cypress.Chainable;

let token = null;

const v2apiFetch = (
  input: string,
  init?: ArgumentTypes<typeof cy.request>[0],
  headers = {}
) => {
  return cy.request({
    url: API_URL + '/v2/' + input,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + token,
      ...headers,
    },
    ...init,
  });
};

const apiFetch = (
  input: string,
  init?: ArgumentTypes<typeof cy.request>[0],
  headers = {}
) => {
  return cy.request({
    url: API_URL + '/api/' + input,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + token,
      ...headers,
    },
    ...init,
  });
};

export const internalFetch = (
  input: string,
  init?: ArgumentTypes<typeof cy.request>[0]
) => {
  return cy.request({
    url: API_URL + '/internal/' + input,
    headers: {
      'Content-Type': 'application/json',
    },
    ...init,
  });
};

export const login = (username = USERNAME, password = PASSWORD) => {
  return cy
    .request({
      url: API_URL + '/api/public/generatetoken',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username,
        password,
      }),
    })
    .then((res) => {
      token = res.body.accessToken;
      window.localStorage.setItem('jwtToken', token);
    });
};

export const createProject = (createProjectDto: {
  name: string;
  languages: Partial<components['schemas']['LanguageDto']>[];
}): Chainable<Cypress.Response> => {
  const create = () =>
    v2apiFetch('projects', {
      body: JSON.stringify(createProjectDto),
      method: 'POST',
    });
  return v2apiFetch('projects').then((res) => {
    const projects = res.body?._embedded?.projects.filter(
      (i) => i.name === createProjectDto.name
    );
    const deletePromises = projects?.map((p) => deleteProject(p.id));

    if (deletePromises) {
      return Cypress.Promise.all(deletePromises).then(() =>
        create()
      ) as any as Chainable<Cypress.Response>;
    }
    return create();
  });
};

export const createTestProject = () =>
  createProject({
    name: 'Test',
    languages: [
      { tag: 'en', name: 'English', originalName: 'English', flagEmoji: '🇬🇧' },
    ],
  });

export const setTranslations = (
  projectId,
  key: string,
  translations: { [lang: string]: string }
) =>
  apiFetch(`project/${projectId}/keys/create`, {
    body: { key, translations },
    method: 'POST',
  });

export const deleteProject = (id: number) => {
  return v2apiFetch(`projects/${id}`, { method: 'DELETE' });
};

export const createUser = (
  username = 'test',
  password = 'test',
  fullName = 'Test Full Name'
) => {
  password = bcrypt.hashSync(password, bcrypt.genSaltSync(10));

  return deleteUser(username).then(() => {
    const sql = `insert into user_account (username, name, password, created_at, updated_at)
                 values ('${username}', '${fullName}', '${password}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`;
    return internalFetch(`sql/execute`, { method: 'POST', body: sql });
  });
};

export const deleteUser = (username: string) => {
  const deleteUserSql = `delete
                         from user_account
                         where username = '${username}'`;
  return internalFetch(`sql/execute`, { method: 'POST', body: deleteUserSql });
};

export const deleteUserWithEmailVerification = (username: string) => {
  const sql = `
      delete
      from permission
      where user_id in (select id from user_account where username = '${username}');
      delete
      from email_verification
      where user_account_id in (select id from user_account where username = '${username}');
      delete
      from user_account
      where username = '${username}';
  `;

  return internalFetch(`sql/execute`, { method: 'POST', body: sql });
};

export const getUser = (username: string) => {
  const sql = `select user_account.username, email_verification.id
               from user_account
                        join email_verification on email_verification.user_account_id = user_account.id
               where username = '${username}'`;
  return internalFetch(`sql/list`, { method: 'POST', body: sql }).then((r) => {
    return r.body[0];
  });
};

export const createApiKey = (body: { projectId: number; scopes: Scope[] }) =>
  v2apiFetch(`api-keys`, { method: 'POST', body }).then(
    (r) => r.body
  ) as any as Promise<ApiKeyDTO>;

export const getAllProjectApiKeys = (projectId: number) =>
  // Cypress Promise implementation is so clever
  // that you cannot resolve undefined or any other falsy value
  // using "chaining" of then methods
  // so we need to wrap the whole fn with another promise to actually
  // resolve empty array
  // thanks Cypress!
  new Promise((resolve) =>
    v2apiFetch(`api-keys`, {
      method: 'GET',
      qs: {
        filterProjectId: projectId,
      },
    }).then((r) => resolve(r.body?._embedded?.apiKeys || []))
  ) as any as Promise<components['schemas']['ApiKeyModel'][]>;

export const deleteAllProjectApiKeys = (projectId: number) =>
  getAllProjectApiKeys(projectId).then((keys) => {
    return keys.forEach((k) =>
      v2apiFetch(`api-keys/${k.id}`, {
        method: 'DELETE',
      })
    );
  });

export const addScreenshot = (projectId: number, key: string, path: string) => {
  return cy.fixture(path).then((f) => {
    const blob = Cypress.Blob.base64StringToBlob(f, 'image/png');
    const data = new FormData();
    data.append('screenshot', blob);
    data.append('key', key);
    cy.log('Uploading screenshot: ' + path);
    return fetch(`${API_URL}/api/project/${projectId}/screenshots`, {
      headers: {
        Authorization: 'Bearer ' + token,
      },
      method: 'POST',
      body: data,
    }).then((r) => {
      if (r.status > 200) {
        r.text().then((t) => cy.log(t));
        throw new Error('Error response from server');
      }
      cy.log('Image uploaded');
    });
  });
};

export const getParsedEmailVerification = () =>
  getAllEmails().then((r) => {
    return {
      verifyEmailLink: r[0].html.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1'),
      fromAddress: r[0].from.value[0].address,
      toAddress: r[0].to.value[0].address,
      text: r[0].text,
    };
  });

export const getParsedEmailInvitationLink = () =>
  getAllEmails().then(
    (r) => r[0].html.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1') as string
  );

export const getAllEmails = () =>
  cy.request('http://localhost:21080/api/emails').then((r) => r.body);
export const deleteAllEmails = () =>
  cy.request({ url: 'http://localhost:21080/api/emails', method: 'DELETE' });

export const getParsedResetPasswordEmail = () =>
  getAllEmails().then((r) => {
    return {
      resetLink: r[0].text.replace(/.*(http:\/\/[\w:/=]*).*/gs, '$1'),
      fromAddress: r[0].from.value[0].address,
      toAddress: r[0].to.value[0].address,
      text: r[0].text,
    };
  });

export const enableEmailVerification = () =>
  setProperty('authentication.needsEmailVerification', true);
export const disableEmailVerification = () =>
  setProperty('authentication.needsEmailVerification', false);

export const enableAuthentication = () =>
  setProperty('authentication.enabled', true);
export const disableAuthentication = () =>
  setProperty('authentication.enabled', false);
export const setProperty = (name: string, value: any) =>
  internalFetch('properties/set', {
    method: 'PUT',
    body: {
      name,
      value,
    },
  });

export const getRecaptchaSiteKey = () => {
  return apiFetch('public/configuration').then(
    (res) => res.body.recaptchaSiteKey
  );
};

export const setRecaptchaSiteKey = (siteKey: string) => {
  setProperty('recaptcha.siteKey', siteKey);
};

export const setRecaptchaSecretKey = (secretKey: string) => {
  setProperty('recaptcha.secretKey', secretKey);
};
