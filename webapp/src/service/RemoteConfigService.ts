import {singleton} from 'tsyringe';
import {RemoteConfigurationDTO} from './response.types';
import {ApiHttpService} from './ApiHttpService';
import {GlobalError} from "../error/GlobalError";
import {ErrorActions} from "../store/global/ErrorActions";

@singleton()
export class RemoteConfigService {
    constructor(private http: ApiHttpService, private errorActions: ErrorActions) {
    }

    public async getConfiguration(): Promise<RemoteConfigurationDTO | undefined> {
        try {
            return await (await this.http.fetch(`public/configuration`)).json();
        } catch (e) {
            this.errorActions.globalError.dispatch(new GlobalError('Error loading configuration.'));
        }
    }
}
