import { API_URL, PASSWORD, USERNAME } from './constants';
import { ArgumentTypes, Scope } from './types';
import { ApiKeyDTO } from '../../../webapp/src/service/response.types';
import { components } from '../../../webapp/src/service/apiSchema.generated';
import bcrypt = require('bcryptjs');

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

const internalFetch = (
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
}) => {
  const create = () =>
    v2apiFetch('projects', {
      body: JSON.stringify(createProjectDto),
      method: 'POST',
    });
  return v2apiFetch('projects').then((res) => {
    const test = res.body?._embeddded?.projects.find(
      (i) => i.name === createProjectDto.name
    );
    if (test) {
      return deleteProject(test.id).then(() => create());
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

export const generateExampleKeys = (
  projectId: number,
  numberOfExamples: number
) => internalFetch(`e2e-data/keys/generate/${projectId}/${numberOfExamples}`);

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
  apiFetch(`apiKeys`, { method: 'POST', body }).then(
    (r) => r.body
  ) as any as Promise<ApiKeyDTO>;

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
      verifyEmailLink: r[0].text.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1'),
      fromAddress: r[0].from.value[0].address,
      toAddress: r[0].to.value[0].address,
      text: r[0].text,
    };
  });

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

export const cleanOrganizationData = () =>
  internalFetch('e2e-data/organizations/clean');
export const createOrganizationData = () =>
  internalFetch('e2e-data/organizations/create');

export const cleanLanguagesData = () =>
  internalFetch('e2e-data/languages/clean');
export const generateLanguagesData = () =>
  internalFetch('e2e-data/languages/generate');

export const cleanImportData = () => internalFetch('e2e-data/import/clean');
export const generateImportData = () =>
  internalFetch('e2e-data/import/generate');
export const generateApplicableImportData = () =>
  internalFetch('e2e-data/import/generate-applicable');
export const generateAllSelectedImportData = () =>
  internalFetch('e2e-data/import/generate-all-selected');
export const generateLotOfImportData = () =>
  internalFetch('e2e-data/import/generate-lot-of-data');
export const generateBaseImportData = () =>
  internalFetch('e2e-data/import/generate-base');
export const generateManyLanguagesImportData = () =>
  internalFetch('e2e-data/import/generate-many-languages');
export const generateWithLongTextImportData = () =>
  internalFetch('e2e-data/import/generate-with-long-text');

export const cleanProjectsData = () => internalFetch('e2e-data/projects/clean');
export const createProjectsData = () =>
  internalFetch('e2e-data/projects/create');

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
