import { T } from '@tolgee/react';
import { singleton } from 'tsyringe';

import { SignUpType } from '../component/security/SignUp/SignUpView';
import { GlobalActions } from '../store/global/GlobalActions';
import { InvitationCodeService } from './InvitationCodeService';
import { MessageService } from './MessageService';
import { TokenService } from './TokenService';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { TokenDTO } from './response.types';

@singleton()
export class SignUpService {
  constructor(
    private http: ApiV1HttpService,
    private tokenService: TokenService,
    private messageService: MessageService,
    private globalActions: GlobalActions,
    private invitationCodeService: InvitationCodeService
  ) {}

  validateEmail = async (email: string): Promise<boolean> => {
    return this.http.post('public/validate_email', email);
  };

  signUp = async (data: SignUpType): Promise<void> => {
    const request = {
      ...data,
      invitationCode: this.invitationCodeService.getCode(),
    } as SignUpType;
    delete request.passwordRepeat;
    try {
      const response = (await this.http.post(
        'public/sign_up',
        request
      )) as TokenDTO;
      if (response.accessToken) {
        this.messageService.success(<T>Thanks for your sign up!</T>);
        this.invitationCodeService.disposeCode();
        this.tokenService.setToken(response.accessToken);
        this.globalActions.setJWTToken.dispatch(response.accessToken);
      }
    } catch (e) {
      if (e.code === 'invitation_code_does_not_exist_or_expired') {
        this.invitationCodeService.disposeCode();
      }
      throw e;
    }
  };

  async verifyEmail(accessToken: string | undefined) {
    this.invitationCodeService.disposeCode();
    if (accessToken) {
      this.tokenService.setToken(accessToken);
      this.globalActions.setJWTToken.dispatch(accessToken);
    }
  }
}
