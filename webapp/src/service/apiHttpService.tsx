import {container, singleton} from 'tsyringe';
import {ErrorActions} from '../store/global/errorActions';
import {RedirectionActions} from '../store/global/redirectionActions';
import {LINKS} from '../constants/links';
import {tokenService} from './tokenService';
import {GlobalError} from "../error/GlobalError";
import {messageService} from "./messageService";
import * as Sentry from '@sentry/browser';
import React from "react";
import {T} from "@polygloat/react";

const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

const API_URL = environment.apiUrl;

let timer;
let requests: { [address: string]: number } = {};
const detectLoop = (url) => {
    requests[url] = 1 + (requests[url] || 0);
    if (requests[url] > 20) {
        return true;
    }
    timer = setTimeout(() => {
        requests = {};
    }, 2000)
};


@singleton()
export class ApiHttpService {
    constructor(private tokenService: tokenService, private messageService: messageService) {
    }

    private static async getResObject(r: Response) {
        const textBody = await r.text();
        try {
            return JSON.parse(textBody);
        } catch (e) {
            return textBody;
        }
    }

    fetch(input: RequestInfo, init?: RequestInit): Promise<Response> {
        if (detectLoop(input)) {
            //if we get into loop, maybe something went wrong in login requests etc, rather start over
            this.tokenService.disposeToken();
            location.reload();
        }
        return new Promise((resolve, reject) => {
            if (this.tokenService.getToken()) {
                init = init || {};
                init.headers = init.headers || {};
                init.headers = {...init.headers, 'Authorization': 'Bearer ' + this.tokenService.getToken()};
            }
            fetch(API_URL + input, init).then((r) => {
                if (r.status == 401) {
                    console.warn('Redirecting to login - unauthorized user');
                    redirectionActions.redirect.dispatch(LINKS.LOGIN.build());
                }
                if (r.status >= 500) {
                    errorActions.globalError.dispatch(new GlobalError('Server responded with error status.'));
                    throw new Error('Error status code from server');
                }
                if (r.status == 403) {
                    redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
                    this.messageService.error(<T>operation_not_permitted_error</T>);
                    Sentry.captureException(new Error("Operation not permitted"));
                    ApiHttpService.getResObject(r).then(b => reject({...b, __handled: true}));
                    return;
                }
                if (r.status == 404) {
                    redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
                    this.messageService.error("Resource not found");
                }
                if (r.status >= 400 && r.status <= 500) {
                    ApiHttpService.getResObject(r).then(b => {
                        reject(b)
                    });
                } else {
                    resolve(r);
                }
            }).catch((e) => {
                console.error(e);
                errorActions.globalError.dispatch(new GlobalError("Error while loading resource", input.toString(), e));
            });
        });
    }

    async get<T>(url, queryObject?: { [key: string]: any }): Promise<T> {
        return ApiHttpService.getResObject(await this.fetch(url + (!queryObject ? "" : "?" + this.buildQuery(queryObject))));
    }

    async getFile(url, queryObject?: { [key: string]: any }): Promise<Blob> {
        const blob = await (await this.fetch(url + (!queryObject ? "" : "?" + this.buildQuery(queryObject)))).blob();
        return blob;
    }

    async post<T>(url, body): Promise<T> {
        return ApiHttpService.getResObject(await (this.postNoJson(url, body)));
    }

    async delete<T>(url, body?: object): Promise<T> {
        return ApiHttpService.getResObject(await this.fetch(url, {
            method: 'DELETE',
            body: body && JSON.stringify(body),
            headers: {
                'Content-Type': 'application/json'
            },
        }));
    }

    async postMultipart<T>(url: string, data: FormData): Promise<T> {
        return ApiHttpService.getResObject(await this.fetch(url, {
            method: 'POST',
            body: data
        }));
    }

    postNoJson(input: RequestInfo, body: {}): Promise<Response> {
        return this.fetch(input, {
            body: JSON.stringify(body),
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
        });
    }

    buildQuery(object: { [key: string]: any }): string {
        return Object.keys(object).filter(k => !!object[k])
            .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(object[k]))
            .join('&');
    }

}
