import { signUpService, SignUpService } from 'tg.service/SignUpService';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';

export class SignUpState extends StateWithLoadables<SignUpActions> {}

export class SignUpActions extends AbstractLoadableActions<SignUpState> {
  readonly loadableDefinitions = {
    signUp: this.createLoadableDefinition((v) => {
      v.callbackUrl = window.location.protocol + '//' + window.location.host;
      return this.service.signUp(v);
    }),
  };

  constructor(private service: SignUpService) {
    super(new SignUpState());
  }

  get prefix(): string {
    return 'SIGN_UP';
  }
}

export const signUpState = new SignUpState();
export const signUpActions = new SignUpActions(signUpService);
