import {AbstractActions} from '../AbstractActions';
import {singleton} from 'tsyringe';

export class RedirectionState {
    to: string = null;
    redirecting: boolean = false;
}

@singleton()
export class RedirectionActions extends AbstractActions<RedirectionState> {

    constructor() {
        super(new RedirectionState());
    }

    redirect = this.createAction('DO', (to: string) => to).build.on(
        (state, action) => ({...state, to: action.payload})
    );

    redirectDone = this.createAction('DONE').build.on(
        (state, action) => ({...state, to: null, redirecting: false})
    );

    get prefix(): string {
        return 'REDIRECT';
    }
}

