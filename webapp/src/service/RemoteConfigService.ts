import {singleton} from 'tsyringe';
import {RemoteConfigurationDTO} from './response.types';
import {ApiHttpService} from './apiHttpService';
import {GlobalError} from "../error/GlobalError";
import {ErrorActions} from "../store/global/errorActions";

@singleton()
export class RemoteConfigService {
    constructor(private http: ApiHttpService, private errorActions: ErrorActions) {
    }

    public async getConfiguration(): Promise<RemoteConfigurationDTO> {
        try {
            return await (await this.http.fetch(`public/configuration`)).json();
        } catch (e) {
            this.errorActions.globalError.dispatch(new GlobalError('Error loading configuration.'));
        }
    }
}
