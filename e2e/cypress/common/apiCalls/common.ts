import { API_URL, PASSWORD, USERNAME } from '../constants';
import { ArgumentTypes, Scope } from '../types';
import { components } from '../../../../webapp/src/service/apiSchema.generated';
import * as bcrypt from 'bcryptjs';
import Chainable = Cypress.Chainable;

type AccountType =
  components['schemas']['PrivateUserAccountModel']['accountType'];
type CreateProjectRequest = components['schemas']['CreateProjectRequest'];

type ImportKeysItemDto = components['schemas']['ImportKeysItemDto'];

let token = null;

export const v2apiFetch = (
  input: string,
  init?: ArgumentTypes<typeof cy.request>[0],
  headers = {}
) => {
  return cy.request({
    url: API_URL + '/v2/' + input,
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? 'Bearer ' + token : undefined,
      ...headers,
    },
    ...init,
  });
};

export const v2apiFetchPromise = (
  input: string,
  init?: Request,
  headers = {}
) => {
  return fetch(API_URL + '/v2/' + input, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? 'Bearer ' + token : undefined,
      ...headers,
    },
    ...init,
  }).then((r) => ({
    ...r,
    body: r.json(),
  }));
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
      Authorization: token ? 'Bearer ' + token : undefined,
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

export const login = (
  username = USERNAME,
  password = PASSWORD,
  otp: string = undefined
) => {
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
        otp,
      }),
    })
    .then((res) => {
      token = res.body.accessToken;
      window.localStorage.setItem('jwtToken', token);
    });
};

export const importData = (projectId: number, data: ImportKeysItemDto[]) => {
  return v2apiFetch(`projects/${projectId}/keys/import`, {
    method: 'post',
    body: JSON.stringify({ keys: data }),
  });
};

export const logout = () => {
  window.localStorage.removeItem('jwtToken');
};

export const getDefaultOrganization = () => {
  return v2apiFetch('organizations').then((res) => {
    const organizations =
      res.body as components['schemas']['PagedModelOrganizationModel'];
    const org = organizations._embedded.organizations[0];
    if (!org) {
      throw Error('No default organization found!');
    }
    return org;
  });
};

export const createProject = (
  createProjectDto: Partial<CreateProjectRequest>
): Chainable<Cypress.Response<any>> => {
  const create = () => {
    return getDefaultOrganization().then((org) => {
      return v2apiFetch('projects', {
        body: JSON.stringify({ ...createProjectDto, organizationId: org.id }),
        method: 'POST',
      });
    });
  };
  return v2apiFetch('projects').then((res) => {
    const projects = res.body?._embedded?.projects.filter(
      (i) => i.name === createProjectDto.name
    );
    const deletePromises = projects?.map((p) => deleteProject(p.id));

    if (deletePromises) {
      return Cypress.Promise.all(deletePromises).then(() =>
        create()
      ) as any as Chainable<Cypress.Response<any>>;
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

export function enableNamespaces(projectId: number) {
  internalFetch(`e2e-data/projects/enable-namespaces?projectId=${projectId}`, {
    method: 'PUT',
  });
  cy.reload();
}

type CreateKeyOptions = {
  isPlural?: boolean;
};

export const createKey = (
  projectId,
  key: string,
  translations: { [lang: string]: string },
  options?: CreateKeyOptions
): Chainable<components['schemas']['KeyModel']> =>
  v2apiFetch(`projects/${projectId}/keys`, {
    body: { ...options, name: key, translations },
    method: 'POST',
  }).then((r) => {
    return r.body;
  });

export const createKeyPromise = (
  projectId,
  key: string,
  translations: { [lang: string]: string }
): Promise<components['schemas']['KeyModel']> =>
  v2apiFetchPromise(`projects/${projectId}/keys`, {
    body: JSON.stringify({ name: key, translations }),
    method: 'POST',
  } as any).then((r) => {
    return r.body;
  });

export const setTranslations = (
  projectId,
  key: string,
  translations: { [lang: string]: string }
) =>
  v2apiFetch(`projects/${projectId}/translations`, {
    body: { key, translations },
    method: 'POST',
  });

export const enableOrganizationsSsoProvider = () =>
  setProperty('authentication.ssoOrganizations.enabled', true);

export const disableOrganizationsSsoProvider = () =>
  setProperty('authentication.ssoOrganizations.enabled', false);

export const enableGlobalSsoProvider = () =>
  setProperty('authentication.ssoGlobal.enabled', true);

export const disableGlobalSsoProvider = () =>
  setProperty('authentication.ssoGlobal.enabled', false);

export const deleteProject = (id: number) => {
  return v2apiFetch(`projects/${id}`, { method: 'DELETE' });
};

export const createUser = (
  username = 'test',
  password = 'test',
  fullName = 'Test Full Name'
) => {
  password = bcrypt.hashSync(password, bcrypt.genSaltSync(10));

  return deleteUserSql(username).then(() => {
    const sql = `insert into user_account (username, name, password, created_at, updated_at)
                 values ('${username}', '${fullName}', '${password}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`;
    return internalFetch(`sql/execute`, { method: 'POST', body: sql });
  });
};

export const deleteUser = () => {
  return v2apiFetch('user', { method: 'delete' });
};

export const deleteUserSql = (username: string) => {
  const sql = `
      delete
      from permission
      where user_id in (select id from user_account where username = '${username}');
      delete
      from email_verification
      where user_account_id in (select id from user_account where username = '${username}');
      delete
      from organization_role
      where user_id in (select id from user_account where username = '${username}');
      delete
      from user_preferences
      where user_account_id in (select id from user_account where username = '${username}');
      delete
      from quick_start
      where user_account_id in (select id from user_account where username = '${username}');
      delete
      from notification
      where user_id in (select id from user_account where username = '${username}');
      delete
      from notification_setting
      where user_id in (select id from user_account where username = '${username}');
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

export const userEnableMfa = (
  username: string,
  key: number[],
  recoveryCodes: string[] = []
) => {
  const encodedKey = key
    .map((byte) => byte.toString(16).toUpperCase().padStart(2, '0'))
    .join('');
  const joinedCodes = recoveryCodes.join(',');

  const sql = `UPDATE user_account
               SET totp_key           = '\\x${encodedKey}',
                   mfa_recovery_codes = '{${joinedCodes}}'
               WHERE username = '${username}'`;
  return internalFetch(`sql/execute`, { method: 'POST', body: sql });
};

export const userDisableMfa = (username: string) => {
  const sql = `UPDATE user_account
               SET totp_key           = NULL,
                   mfa_recovery_codes = '{}'
               WHERE username = '${username}'`;
  return internalFetch(`sql/execute`, { method: 'POST', body: sql });
};

export const setUserType = (username: string, type: AccountType) => {
  const sql = `UPDATE user_account
               SET account_type = '${type}'
               WHERE username = '${username}'`;
  return internalFetch(`sql/execute`, { method: 'POST', body: sql });
};

export const createApiKey = (body: { projectId: number; scopes: Scope[] }) =>
  v2apiFetch(`api-keys`, { method: 'POST', body }).then(
    (r) => r.body
  ) as any as Promise<components['schemas']['ApiKeyModel']>;

export const getAllProjectApiKeys = (projectId: number) =>
  // Cypress Promise implementation is so clever
  // that you cannot resolve undefined or any other falsy value
  // using "chaining" of then methods
  // so we need to wrap the whole fn with another promise to actually
  // resolve empty array
  // thanks Cypress!
  new Promise<components['schemas']['ApiKeyModel'][]>((resolve) =>
    v2apiFetch(`api-keys`, {
      method: 'GET',
      qs: {
        filterProjectId: projectId,
      },
    }).then((r) => resolve(r.body?._embedded?.apiKeys || []))
  );

export const deleteAllProjectApiKeys = (projectId: number) =>
  getAllProjectApiKeys(projectId).then((keys) => {
    return keys.forEach((k) =>
      v2apiFetch(`api-keys/${k.id}`, {
        method: 'DELETE',
      })
    );
  });

export const addScreenshot = (
  projectId: number,
  keyId: number,
  path: string
) => {
  return cy.fixture(path).then((f) => {
    const blob = Cypress.Blob.base64StringToBlob(f, 'image/png');
    const data = new FormData();
    data.append('screenshot', blob);
    return fetch(
      `${API_URL}/v2/projects/${projectId}/keys/${keyId}/screenshots`,
      {
        headers: {
          Authorization: 'Bearer ' + token,
        },
        method: 'POST',
        body: data,
      }
    ).then((r) => {
      if (r.status >= 400) {
        // eslint-disable-next-line no-console
        r.text().then((t) => console.error(t));
        throw new Error('Error response from server');
      }
    });
  });
};

export const getLastEmail = () =>
  getLatestEmail().then((r) => {
    return {
      fromAddress: r.From.Address,
      toAddress: r.To[0].Address,
      subject: r.Subject,
      html: r.HTML,
    };
  });

export const getAssignedEmailNotification = () =>
  getLatestEmail().then((r) => {
    const content = r.HTML;
    const result = [...content.matchAll(/href="(.*?)"/g)];
    return {
      taskLink: result[0][1],
      myTasksLink: result[2][1],
      fromAddress: r.From.Address,
      toAddress: r.To[0].Address,
      content: r.HTML,
    };
  });

export const getParsedEmailVerification = () =>
  getLatestEmail().then((r) => {
    return {
      verifyEmailLink: r.HTML.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1'),
      fromAddress: r.From.Address,
      toAddress: r.To[0].Address,
      content: r.HTML,
    };
  });

export const getParsedEmailVerificationByIndex = (index: number) => {
  if (index === 0) {
    return getLatestEmail().then((email) => {
      return {
        verifyEmailLink: email.HTML.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1'),
        fromAddress: email.From.Address,
        toAddress: email.To[0].Address,
        content: email.HTML,
      };
    });
  } else {
    return getAllEmails().then((emails) => {
      return getEmail(emails[index].ID).then((email) => {
        return {
          verifyEmailLink: email.HTML.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1'),
          fromAddress: email.From.Address,
          toAddress: email.To[0].Address,
          content: email.HTML,
        };
      });
    });
  }
};

export const getParsedEmailInvitationLink = () =>
  getLatestEmail().then(
    (email) => email.HTML.replace(/.*(http:\/\/[\w:/]*).*/gs, '$1') as string
  );

export const getAgencyInvitationLinks = () =>
  getAllEmails().then((emails) => {
    const email = emails.find((e) =>
      e.Subject.includes('New translation request')
    );
    return getEmail(email.ID).then((e) => {
      const links = Array.from(
        e.HTML.matchAll(/(http:\/\/[\w:/]*)/g),
        (m) => m[0]
      );

      const invitation = links.find((l) => l.includes('accept_invitation'));
      const project = links.find(
        (l) => l.includes('/projects/') && !l.includes('/task')
      );
      const tasks = links.filter((l) => l.includes('/task'));
      return {
        invitation,
        project,
        tasks,
      };
    });
  });

export const getOrderConfirmation = () =>
  getAllEmails().then((emails) => {
    const email = emails.find((e) =>
      e.Subject.includes('Your translation order to')
    );
    return getEmail(email.ID).then((e) => {
      const links = Array.from(
        e.HTML.matchAll(/(http:\/\/[\w:/]*)/g),
        (m) => m[0]
      );
      const project = links.find(
        (l) => l.includes('/projects/') && !l.includes('/task')
      );
      const tasks = links.filter((l) => l.includes('/task'));
      return {
        project,
        tasks,
        content: e.HTML,
      };
    });
  });

type Email = {
  ID: string;
  To: any;
  From: any;
  Subject: string;
};

type EmailSummary = {
  HTML: string;
  Subject: string;
  To: any;
  From: any;
};

function fetchEmails(limit = 0) {
  let options = { url: 'http://localhost:21080/api/v1/messages' };
  if (limit) {
    options = { ...options, ...{ qs: { limit } } };
  }
  return cy.request(options).then((r) => {
    return r.body.messages as Email[];
  });
}

export const getAllEmails = () => fetchEmails();

export const getLatestEmail = (): Cypress.Chainable<EmailSummary> => {
  const promise = new Cypress.Promise<EmailSummary>((resolve, reject) => {
    const attempt = (count: number) => {
      cy.request({
        url: 'http://localhost:21080/api/v1/message/latest',
        failOnStatusCode: false,
      }).then((r) => {
        const body = r.body as EmailSummary | undefined;
        const hasMessage =
          r.status === 200 && body && (body.HTML || body.Subject);

        if (hasMessage) {
          resolve(body!);
          return;
        }

        if (count < 3) {
          cy.wait(250).then(() => attempt(count + 1));
        } else {
          reject(
            new Error(
              `Failed to fetch latest email after ${count + 1} attempt(s).`
            )
          );
        }
      });
    };

    attempt(0);
  });

  return cy.wrap(promise);
};

export const getEmail = (id) =>
  cy
    .request({ url: `http://localhost:21080/api/v1/message/${id}` })
    .then((r) => r.body as EmailSummary);

export const deleteAllEmails = () =>
  cy.request({
    url: 'http://localhost:21080/api/v1/messages',
    method: 'DELETE',
  });

export const getParsedResetPasswordEmail = () =>
  getLatestEmail().then((r) => {
    return {
      resetLink: r.HTML.replace(/.*(http:\/\/[\w:/=]*).*/gs, '$1'),
      fromAddress: r.From.Address,
      toAddress: r.To[0].Address,
    };
  });

export const enableEmailVerification = () =>
  setProperty('authentication.needsEmailVerification', true);
export const disableEmailVerification = () =>
  setProperty('authentication.needsEmailVerification', false);

export const enableRegistration = () =>
  setProperty('authentication.registrationsAllowed', true);
export const disableRegistration = () =>
  setProperty('authentication.registrationsAllowed', false);

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

export const setBypassSeatCountCheck = (value: boolean) =>
  internalFetch('bypass-seat-count-check/set', {
    method: 'PUT',
    qs: { value: value ? 'true' : 'false' },
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

export const forceDate = (timestamp: number) => {
  internalFetch(`time/${timestamp}`, { method: 'PUT' });
};

export const releaseForcedDate = () => {
  internalFetch(`time`, { method: 'DELETE' });
};

export const setContentStorageBypass = (value: boolean) =>
  setProperty('internal.e3eContentStorageBypassOk', value);

export const setWebhookControllerStatus = (value: number) =>
  setProperty('internal.webhookControllerStatus', value);

export const setTranslationsViewLanguagesLimit = (value: number) =>
  setProperty('translationsViewLanguagesLimit', value);
