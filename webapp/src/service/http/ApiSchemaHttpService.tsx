import { paths } from '../apiSchema.generated';

import { ApiHttpService, RequestOptions } from './ApiHttpService';

export class ApiSchemaHttpService extends ApiHttpService {
  apiUrl = import.meta.env.VITE_APP_API_URL as string;

  schemaRequest<
    Url extends keyof Paths,
    Method extends keyof Paths[Url],
    Paths = paths
  >(url: Url, method: Method, options?: RequestOptions) {
    return async (request: RequestParamsType<Url, Method, Paths>) => {
      const response = await ApiHttpService.getResObject(
        await this.schemaRequestRaw<Url, Method, Paths>(
          url,
          method,
          options
        )(request),
        options
      );
      return response as Promise<ResponseContent<Url, Method, Paths>>;
    };
  }

  schemaRequestRaw<
    Url extends keyof Paths,
    Method extends keyof Paths[Url],
    Paths
  >(url: Url, method: Method, options?: RequestOptions) {
    return async (request: RequestParamsType<Url, Method, Paths>) => {
      const pathParams = request?.path;
      let urlResult = url as string;

      if (pathParams) {
        Object.entries(pathParams).forEach(([key, value]) => {
          urlResult = urlResult.replace(`{${key}}`, value);
        });
      }

      const formData = request?.content?.['multipart/form-data'] as Record<
        string,
        unknown
      >;
      let body: FormData | undefined = undefined;
      if (formData) {
        body = new FormData();
        Object.entries(formData).forEach(([key, value]) => {
          if (Array.isArray(value)) {
            let fileName: undefined | string = undefined;
            if (Object.prototype.toString.call(value) === '[object File]') {
              fileName = (value as any as File).name;
            }

            value.forEach((item) => body?.append(key, item as any, fileName));
            return;
          }
          body?.append(key, value as any);
        });
      }

      const jsonBody = JSON.stringify(request?.content?.['application/json']);

      const queryParams = request?.query;
      let queryString = '';

      const params = flattenParams(queryParams);
      const query = this.buildQuery(params);
      if (query) {
        queryString = '?' + query;
      }

      return await this.fetch(
        urlResult + queryString,
        {
          method: method as string,
          body: body || jsonBody,
          headers: jsonBody
            ? {
                'Content-Type': 'application/json',
              }
            : undefined,
          signal: options?.signal,
        },
        options
      );
    };
  }
}

type Params = {
  [k: string]: string | string[] | null | undefined | Params;
};

const flattenParams = (params: Params | null | undefined) => {
  if (params) {
    return Object.entries(params).reduce(
      (acc, [key, value]) =>
        Array.isArray(value) || typeof value !== 'object'
          ? { ...acc, [key]: value }
          : { ...acc, ...flattenParams(value) },
      {}
    );
  } else {
    return {};
  }
};

export type RequestParamsType<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = OperationSchema<Url, Method, Paths>['parameters'] &
  OperationSchema<Url, Method, Paths>['requestBody'];

export type ResponseContent<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths
> = OperationSchema<
  Url,
  Method,
  Paths
>['responses'][200] extends NotNullAnyContent
  ? OperationSchema<Url, Method, Paths>['responses'][200]['content']['*/*']
  : OperationSchema<
      Url,
      Method,
      Paths
    >['responses'][200] extends NotNullJsonHalContent
  ? OperationSchema<
      Url,
      Method,
      Paths
    >['responses'][200]['content']['application/hal+json']
  : OperationSchema<
      Url,
      Method,
      Paths
    >['responses'][200] extends NotNullJsonContent
  ? OperationSchema<
      Url,
      Method,
      Paths
    >['responses'][200]['content']['application/json']
  : OperationSchema<
      Url,
      Method,
      Paths
    >['responses'][201] extends NotNullAnyContent
  ? OperationSchema<Url, Method, Paths>['responses'][201]['content']['*/*']
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
  200?:
    | {
        content?: {
          '*/*'?: any;
          'application/json'?: any;
          'application/hal+json'?: any;
        };
      }
    | unknown;
  201?:
    | {
        content?: {
          '*/*'?: any;
        };
      }
    | unknown;
};

type OperationSchemaType = {
  requestBody?: {
    content?: {
      'multipart/form-data'?: { [key: string]: any };
      'application/json'?: any;
    };
  };
  parameters?: {
    path?: { [key: string]: any };
    query?: { [key: string]: { [key: string]: any } | any };
  };
  responses: ResponseType;
};

type OperationSchema<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = Paths[Url][Method] extends OperationSchemaType ? Paths[Url][Method] : never;

export const apiSchemaHttpService = new ApiSchemaHttpService();
