import { AbstractActions } from '../AbstractActions';

export class RedirectionState {
  to: string | null = null;
  redirecting = false;
}

export class RedirectionActions extends AbstractActions<RedirectionState> {
  constructor() {
    super(new RedirectionState());
  }

  redirect = this.createAction('DO', (to: string) => to).build.on(
    (state, action) => ({ ...state, to: action.payload })
  );

  redirectDone = this.createAction('DONE').build.on((state, action) => ({
    ...state,
    to: null,
    redirecting: false,
  }));

  get prefix(): string {
    return 'REDIRECT';
  }
}

export const redirectionActions = new RedirectionActions();
