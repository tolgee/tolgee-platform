import {AbstractActions} from '../AbstractActions';
import {singleton} from 'tsyringe';
import {GlobalError} from "../../error/GlobalError";

export class ErrorState {
    error: GlobalError = null;
}

@singleton()
export class ErrorActions extends AbstractActions<ErrorState> {

    constructor() {
        super(new ErrorState());
    }

    globalError = this.createAction('ERROR', (e: GlobalError) => e).build.on((state, action) =>
        ({...state, error: action.payload}));

    get prefix(): string {
        return 'ERROR';
    }
}

