import { GlobalError } from 'tg.error/GlobalError';
import { singleton } from 'tsyringe';
import { AbstractActions } from '../AbstractActions';

export class ErrorState {
  error: GlobalError | null = null;
}

@singleton()
export class ErrorActions extends AbstractActions<ErrorState> {
  constructor() {
    super(new ErrorState());
  }

  globalError = this.createAction('ERROR', (e: GlobalError) => e).build.on(
    (state, action) => ({ ...state, error: action.payload })
  );

  get prefix(): string {
    return 'ERROR';
  }
}
