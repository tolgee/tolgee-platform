import { singleton } from 'tsyringe';

import { ErrorActions } from '../store/global/ErrorActions';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { components } from 'tg.service/apiSchema.generated';

@singleton()
export class RemoteConfigService {
  constructor(
    private http: ApiV1HttpService,
    private errorActions: ErrorActions
  ) {}

  public async getConfiguration(): Promise<
    components['schemas']['PublicConfigurationDTO'] | undefined
  > {
    return await (await this.http.fetch(`public/configuration`)).json();
  }
}
