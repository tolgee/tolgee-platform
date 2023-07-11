import * as Sentry from '@sentry/browser';
import { T } from '@tolgee/react';
import { container, singleton } from 'tsyringe';

import { LINKS } from 'tg.constants/links';
import { GlobalError } from 'tg.error/GlobalError';
import { ErrorActions } from 'tg.store/global/ErrorActions';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';

import { MessageService } from '../MessageService';
import { TokenService } from '../TokenService';
import { errorCapture } from './errorCapture';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { getUtmCookie } from 'tg.fixtures/utmCookie';

const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

let requests: { [address: string]: number } = {};
const detectLoop = (url) => {
  requests[url] = 1 + (requests[url] || 0);
  if (requests[url] > 30) {
    return true;
  }
  setTimeout(() => {
    requests = {};
  }, 20000);
};

export class RequestOptions {
  disableNotFoundHandling? = false;
  disableAuthHandling? = false;
  disableBadRequestHandling? = false;
  asBlob? = false;
  signal?: any;
}

@singleton()
export class ApiHttpService {
  constructor(
    private tokenService: TokenService,
    private messageService: MessageService,
    private redirectionActions: RedirectionActions
  ) {}

  apiUrl = process.env.REACT_APP_API_URL + '/api/';

  fetch(
    input: RequestInfo,
    init?: RequestInit,
    options: RequestOptions = new RequestOptions()
  ): Promise<Response> {
    if (detectLoop(input)) {
      //if we get into loop, maybe something went wrong in login requests etc, rather start over
      this.tokenService.disposeToken();
      this.redirectionActions.redirect.dispatch(LINKS.PROJECTS.build());
      location.reload();
    }
    return new Promise((resolve, reject) => {
      const fetchIt = () => {
        const jwtToken = this.tokenService.getToken();
        if (jwtToken) {
          init = init || {};
          init.headers = init.headers || {};
          init.headers = {
            ...init.headers,
            Authorization: 'Bearer ' + jwtToken,
          };
          addUtmHeader(init.headers!);
        }

        fetch(this.apiUrl + input, init)
          .then(async (r) => {
            if (r.status >= 400) {
              const resObject = await ApiHttpService.getResObject(r);
              if (r.status == 401 && !options.disableAuthHandling) {
                // eslint-disable-next-line no-console
                console.warn('Redirecting to login - unauthorized user');
                this.messageService.error(<T keyName="expired_jwt_token" />);
                redirectionActions.redirect.dispatch(LINKS.LOGIN.build());
                this.tokenService.disposeToken();
                location.reload();
                return;
              }
              if (r.status >= 500) {
                const message =
                  '500: ' +
                  (resObject?.message || 'Error status code from server');
                errorActions.globalError.dispatch(new GlobalError(message));
                throw new Error(message);
              }
              if (r.status == 403) {
                if (resObject.code === 'expired_super_jwt_token') {
                  container.resolve(GlobalActions).requestSuperJwt.dispatch({
                    onSuccess: () => {
                      fetchIt();
                    },
                    onCancel: () => {
                      reject({ code: 'authentication_cancelled' });
                    },
                  });
                  return;
                }
              }
              if (r.status == 403 && !options.disableAuthHandling) {
                if (init?.method === undefined || init?.method === 'get') {
                  redirectionActions.redirect.dispatch(
                    LINKS.AFTER_LOGIN.build()
                  );
                }
                this.messageService.error(
                  <T keyName="operation_not_permitted_error" />
                );
                Sentry.captureException(new Error('Operation not permitted'));
                reject({ ...resObject, __handled: true });
                return;
              }
              if (r.status == 404 && !options.disableNotFoundHandling) {
                if (init?.method === undefined || init?.method === 'get') {
                  redirectionActions.redirect.dispatch(
                    LINKS.AFTER_LOGIN.build()
                  );
                }
                this.messageService.error(
                  <T keyName="resource_not_found_message" />
                );
              }
              if (r.status == 400 && !options.disableBadRequestHandling) {
                this.messageService.error(
                  <TranslatedError code={parseErrorResponse(resObject)[0]} />
                );
              }
              if (r.status >= 400 && r.status <= 500) {
                errorCapture(resObject.code);
                reject(resObject);
                return;
              }
            }
            resolve(r);
          })
          .catch((e) => {
            if (e instanceof DOMException) {
              if (e.name === 'AbortError') {
                reject('aborted');
                return;
              }
            }
            // eslint-disable-next-line no-console
            console.error(e);
            errorActions.globalError.dispatch(
              new GlobalError(
                'Error while loading resource',
                input.toString(),
                e
              )
            );
            reject(e);
          });
      };
      fetchIt();
    });
  }

  async get<T = any>(
    url,
    queryObject?: {
      [key: string]: any;
    }
  ): Promise<T> {
    return ApiHttpService.getResObject(
      await this.fetch(
        url + (!queryObject ? '' : '?' + this.buildQuery(queryObject))
      )
    );
  }

  async getFile(
    url,
    queryObject?: {
      [key: string]: any;
    }
  ): Promise<Blob> {
    return await (
      await this.fetch(
        url + (!queryObject ? '' : '?' + this.buildQuery(queryObject))
      )
    ).blob();
  }

  async post<T = any>(url, body): Promise<T> {
    return ApiHttpService.getResObject(await this.postNoJson(url, body));
  }

  async put<T = any>(url, body): Promise<T> {
    return ApiHttpService.getResObject(await this.putNoJson(url, body));
  }

  async delete<T = any>(url, body?: any): Promise<T> {
    return ApiHttpService.getResObject(
      await this.fetch(url, {
        method: 'DELETE',
        body: body && JSON.stringify(body),
        headers: {
          'Content-Type': 'application/json',
        },
      })
    );
  }

  async postMultipart<T>(url: string, data: FormData): Promise<T> {
    return ApiHttpService.getResObject(
      await this.fetch(url, {
        method: 'POST',
        body: data,
      })
    );
  }

  postNoJson(input: RequestInfo, body: any): Promise<Response> {
    return this.fetch(input, {
      body: JSON.stringify(body),
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  putNoJson(input: RequestInfo, body: any): Promise<Response> {
    return this.fetch(input, {
      body: JSON.stringify(body),
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  buildQuery(object: { [key: string]: any }): string {
    return Object.keys(object)
      .filter((k) => !!object[k])
      .map((k) => {
        if (Array.isArray(object[k])) {
          return object[k]
            .map(
              (v) =>
                encodeURIComponent(k) +
                '=' +
                (v === '' ? '%02%03' : encodeURIComponent(v))
            )
            .join('&');
        } else {
          return encodeURIComponent(k) + '=' + encodeURIComponent(object[k]);
        }
      })
      .join('&');
  }

  static async getResObject(r: Response, o?: RequestOptions) {
    if (o?.asBlob) {
      return r.blob();
    }

    const textBody = await r.text();
    try {
      return JSON.parse(textBody);
    } catch (e) {
      return textBody;
    }
  }
}

function addUtmHeader(headers: HeadersInit) {
  const cookie = getUtmCookie();
  if (cookie) {
    headers['X-Tolgee-Utm'] = cookie;
  }
}
