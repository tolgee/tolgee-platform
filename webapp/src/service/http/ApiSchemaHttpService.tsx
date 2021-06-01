import { singleton } from 'tsyringe';
import { TokenService } from '../TokenService';
import { MessageService } from '../MessageService';
import React from 'react';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { paths } from '../apiSchema';
import { ApiHttpService, RequestOptions } from './ApiHttpService';

@singleton()
export class ApiSchemaHttpService extends ApiHttpService {
  constructor(
    tokenService: TokenService,
    messageService: MessageService,
    redirectionActions: RedirectionActions
  ) {
    super(tokenService, messageService, redirectionActions);
  }

  apiUrl = process.env.REACT_APP_API_URL as string;

  schemaRequest<Url extends keyof paths, Method extends keyof paths[Url]>(
    url: Url,
    method: Method,
    options?: RequestOptions
  ) {
    return async (request: RequestParamsType<Url, Method>) => {
      const response = await ApiHttpService.getResObject(
        await this.schemaRequestRaw(url, method, options)(request)
      );
      return response as Promise<ResponseContent<Url, Method>>;
    };
  }

  schemaRequestRaw<Url extends keyof paths, Method extends keyof paths[Url]>(
    url: Url,
    method: Method,
    options?: RequestOptions
  ) {
    return async (request: RequestParamsType<Url, Method>) => {
      const pathParams = request?.path;
      let urlResult = url as string;

      if (pathParams) {
        Object.entries(pathParams).forEach(([key, value]) => {
          urlResult = urlResult.replace(`{${key}}`, value);
        });
      }

      const formData = request?.content?.['multipart/form-data'] as {};
      let body: FormData | undefined = undefined;
      if (formData) {
        body = new FormData();
        Object.entries(formData).forEach(([key, value]) => {
          if (Array.isArray(value)) {
            let fileName: undefined | string = undefined;
            if (Object.prototype.toString.call(value) === '[object File]') {
              fileName = (value as any as File).name;
            }

            value.forEach((item) => body!.append(key, item as any, fileName));
            return;
          }
          body!.append(key, value as any);
        });
      }

      const queryParams = request?.query;
      let queryString = '';

      if (queryParams) {
        const params = Object.entries(queryParams).reduce(
          (acc, [key, value]) =>
            typeof value === 'object'
              ? { ...acc, ...value }
              : { ...acc, [key]: value },
          {}
        );
        queryString = '?' + this.buildQuery(params);
      }

      return await this.fetch(
        urlResult + queryString,
        { method: method as string, body: body },
        options
      );
    };
  }
}

type RequestParamsType<
  Url extends keyof paths,
  Method extends keyof paths[Url]
> = OperationSchema<Url, Method>['parameters'] &
  OperationSchema<Url, Method>['requestBody'];

type ResponseContent<Url extends keyof paths, Method extends keyof paths[Url]> =
  OperationSchema<Url, Method>['responses'][200] extends NotNullAnyContent
    ? OperationSchema<Url, Method>['responses'][200]['content']['*/*']
    : OperationSchema<
        Url,
        Method
      >['responses'][200] extends NotNullJsonHalContent
    ? OperationSchema<
        Url,
        Method
      >['responses'][200]['content']['application/hal+json']
    : OperationSchema<Url, Method>['responses'][200] extends NotNullJsonContent
    ? OperationSchema<
        Url,
        Method
      >['responses'][200]['content']['application/json']
    : void;

type NotNullAnyContent = {
  content: {
    '*/*': any;
  };
};

type NotNullJsonHalContent = {
  content: {
    'application/hal+json': any;
  };
};

type NotNullJsonContent = {
  content: {
    'application/json': any;
  };
};

type ResponseType = {
  200:
    | {
        content?: {
          '*/*'?: any;
          'application/json'?: any;
          'application/hal+json'?: any;
        };
      }
    | unknown;
};

type OperationSchemaType = {
  requestBody?: {
    content?: {
      'multipart/form-data'?: { [key: string]: any };
    };
  };
  parameters?: {
    path?: { [key: string]: any };
    query?: { [key: string]: { [key: string]: any } | any };
  };
  responses: ResponseType;
};

type OperationSchema<Url extends keyof paths, Method extends keyof paths[Url]> =
  paths[Url][Method] extends OperationSchemaType ? paths[Url][Method] : never;
