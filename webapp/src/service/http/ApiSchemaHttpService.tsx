import {singleton} from 'tsyringe';
import {TokenService} from '../TokenService';
import {MessageService} from "../MessageService";
import React from "react";
import {ApiV1HttpService} from "./ApiV1HttpService";
import {RedirectionActions} from "../../store/global/RedirectionActions";
import {paths} from "../apiSchema";
import {ApiHttpService} from "./ApiHttpService";

@singleton()
export class ApiSchemaHttpService extends ApiHttpService {
    constructor(tokenService: TokenService, messageService: MessageService, redirectionActions: RedirectionActions) {
        super(tokenService, messageService, redirectionActions)
    }

    apiUrl = process.env.REACT_APP_API_URL as string

    schemaRequest<Url extends keyof paths, Method extends keyof paths[Url]>(url: Url, method: Method) {

        return async (request: OperationSchema<Url, Method>["parameters"]) => {
            const pathParams = request?.path;
            let urlResult = url as string;

            if (pathParams) {
                Object.entries(pathParams).forEach(([key, value]) => {
                    urlResult = urlResult.replace(`{${key}}`, value)
                })
            }

            const queryParams = request?.query;
            let queryString = ""

            if (queryParams) {
                const params = Object.entries(queryParams).reduce((acc, [key, value]) =>
                        typeof value === "object" ? {...acc, ...value} : {...acc, [key]: value},
                    {})
                queryString = this.buildQuery(params)
            }

            const response = await ApiHttpService.getResObject(await this.fetch(urlResult + queryString, {method: method as string}))
            return response as Promise<ResponseContent<Url, Method>>
        }
    }
}


type ResponseContent<Url extends keyof paths, Method extends keyof paths[Url]> =
    OperationSchema<Url, Method>["responses"][200] extends NotNullContent ? OperationSchema<Url, Method>["responses"][200]["content"]["*/*"] : void;

type NotNullContent = {
    content: {
        "*/*": any
    }
}

type ResponseType = {
    200: {
        content?: {
            "*/*": any
        }
    } | unknown
}

type OperationSchemaType = {
    parameters?: {
        path?: { [key: string]: any },
        query?: { [key: string]: { [key: string]: any } | string }
    }
    responses: ResponseType
}

type OperationSchema<Url extends keyof paths, Method extends keyof paths[Url]> = paths[Url][Method] extends OperationSchemaType ? paths[Url][Method] : never

