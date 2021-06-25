import { singleton } from 'tsyringe';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';
import { TokenService } from '../TokenService';
import { MessageService } from '../MessageService';
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
