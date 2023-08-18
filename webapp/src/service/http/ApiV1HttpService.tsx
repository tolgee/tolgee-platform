import { singleton } from 'tsyringe';

import { RedirectionActions } from 'tg.store/global/RedirectionActions';

import { TokenService } from '../TokenService';
import { ApiHttpService } from './ApiHttpService';

@singleton()
export class ApiV1HttpService extends ApiHttpService {
  constructor(
    tokenService: TokenService,
    redirectionActions: RedirectionActions
  ) {
    super(tokenService, redirectionActions);
  }
  apiUrl = process.env.REACT_APP_API_URL + '/api/';
}
