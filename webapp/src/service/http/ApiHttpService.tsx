import { LINKS } from 'tg.constants/links';
import { GlobalError } from 'tg.error/GlobalError';
import { errorActions } from 'tg.store/global/ErrorActions';
import { redirectionActions } from 'tg.store/global/RedirectionActions';

import { tokenService } from '../TokenService';
import { globalActions } from 'tg.store/global/GlobalActions';
import { getUtmCookie } from 'tg.fixtures/utmCookie';
import { handleApiError } from './handleApiError';
import { ApiError } from './ApiError';
import { errorAction } from './errorAction';

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
  asBlob? = false;
  signal?: any;
  disableAutoErrorHandle? = false;
  disableAuthRedirect? = false;
  disableErrorNotification? = false;
  disable404Redirect? = false;
}

export class ApiHttpService {
  apiUrl = import.meta.env.VITE_APP_API_URL + '/api/';

  fetch(
    input: RequestInfo,
    init?: RequestInit,
    options: RequestOptions = new RequestOptions()
  ): Promise<Response> {
    if (detectLoop(input)) {
      //if we get into loop, maybe something went wrong in login requests etc, rather start over
      tokenService.disposeToken();
      redirectionActions.redirect.dispatch(LINKS.PROJECTS.build());
      location.reload();
    }
    return new Promise((resolve, reject) => {
      const fetchIt = () => {
        const jwtToken = tokenService.getToken();
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
              const responseData = await ApiHttpService.getResObject(r);
              const resultError = new ApiError('Api error', responseData);
              resultError.setErrorHandler(() =>
                handleApiError(r, responseData, init, options)
              );
              if (r.status === 400) {
                errorAction(responseData.code);
              }

              if (
                r.status == 403 &&
                responseData.code === 'expired_super_jwt_token'
              ) {
                globalActions.requestSuperJwt.dispatch({
                  onSuccess: () => {
                    fetchIt();
                  },
                  onCancel: () => {
                    reject({ code: 'authentication_cancelled' });
                  },
                });
              } else {
                if (!options.disableAutoErrorHandle) {
                  resultError.handleError?.();
                  reject(resultError);
                } else {
                  reject(resultError);
                }
              }
            } else {
              resolve(r);
            }
          })
          .catch((e) => {
            if (e instanceof DOMException) {
              if (e.name === 'AbortError') {
                reject(new Error('aborted'));
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

export const apiHttpService = new ApiHttpService();
