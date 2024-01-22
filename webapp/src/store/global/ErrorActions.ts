import { GlobalError } from 'tg.error/GlobalError';

import { AbstractActions } from '../AbstractActions';

export class ErrorState {
  error: GlobalError | null = null;
}

export class ErrorActions extends AbstractActions<ErrorState> {
  constructor() {
    super(new ErrorState());
  }

  globalError = this.createAction(
    'ERROR',
    (e: GlobalError | null) => e
  ).build.on((state, action) => ({ ...state, error: action.payload }));

  get prefix(): string {
    return 'ERROR';
  }
}

export const errorActions = new ErrorActions();
