import { singleton } from 'tsyringe';

import { GlobalError } from '../error/GlobalError';
import { ErrorActions } from '../store/global/ErrorActions';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { RemoteConfigurationDTO } from './response.types';

@singleton()
export class RemoteConfigService {
  constructor(
    private http: ApiV1HttpService,
    private errorActions: ErrorActions
  ) {}

  public async getConfiguration(): Promise<RemoteConfigurationDTO | undefined> {
    try {
      return await (await this.http.fetch(`public/configuration`)).json();
    } catch (e) {
      this.errorActions.globalError.dispatch(
        new GlobalError('Error loading configuration.')
      );
    }
  }
}
