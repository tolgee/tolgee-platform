import {
  redirectionActions,
  RedirectionActions,
} from 'tg.store/global/RedirectionActions';

import { messageService, MessageService } from '../MessageService';
import { tokenService, TokenService } from '../TokenService';
import { ApiV1HttpService } from './ApiV1HttpService';

export class ApiV2HttpService extends ApiV1HttpService {
  constructor(
    tokenService: TokenService,
    messageService: MessageService,
    redirectionActions: RedirectionActions
  ) {
    super(tokenService, messageService, redirectionActions);
  }

  apiUrl = import.meta.env.VITE_API_URL + '/v2/';
}

export const apiV2HttpService = new ApiV2HttpService(
  tokenService,
  messageService,
  redirectionActions
);
