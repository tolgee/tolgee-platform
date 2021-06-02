import { singleton } from 'tsyringe';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { TokenService } from '../TokenService';
import { MessageService } from '../MessageService';
import React from 'react';
import { paths } from '../apiSchema';
import { ApiHttpService } from './ApiHttpService';

@singleton()
export class ApiV1HttpService extends ApiHttpService {
  constructor(
    tokenService: TokenService,
    messageService: MessageService,
    redirectionActions: RedirectionActions
  ) {
    super(tokenService, messageService, redirectionActions);
  }
  apiUrl = process.env.REACT_APP_API_URL + '/api/';
}

type ResponseContent<Url extends keyof paths, Method extends keyof paths[Url]> =
  OperationSchema<Url, Method>['responses'][200] extends NotNullContent
    ? OperationSchema<Url, Method>['responses'][200]['content']['*/*']
    : void;

type NotNullContent = {
  content: {
    '*/*': any;
  };
};

type ResponseType = {
  200:
    | {
        content?: {
          '*/*': any;
        };
      }
    | unknown;
};

type OperationSchemaType = {
  parameters?: {
    path?: { [key: string]: any };
    query?: { [key: string]: { [key: string]: any } };
  };
  responses: ResponseType;
};

type OperationSchema<Url extends keyof paths, Method extends keyof paths[Url]> =
  paths[Url][Method] extends OperationSchemaType ? paths[Url][Method] : never;
