import {
  redirectionActions,
  RedirectionActions,
} from 'tg.store/global/RedirectionActions';

import { messageService, MessageService } from '../MessageService';
import { tokenService, TokenService } from '../TokenService';
import { ApiHttpService } from './ApiHttpService';

export class ApiV1HttpService extends ApiHttpService {
  constructor(
    tokenService: TokenService,
    messageService: MessageService,
    redirectionActions: RedirectionActions
  ) {
    super(tokenService, messageService, redirectionActions);
  }
  apiUrl = import.meta.env.VITE_API_URL + '/api/';
}

export const apiV1HttpService = new ApiV1HttpService(
  tokenService,
  messageService,
  redirectionActions
);
