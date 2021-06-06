import { singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { TokenDTO } from './response.types';
import { TokenService } from './TokenService';
import { MessageService } from './MessageService';
import { SignUpType } from '../component/security/SignUpView';
import { RedirectionActions } from '../store/global/RedirectionActions';
import { GlobalActions } from '../store/global/GlobalActions';
import { InvitationCodeService } from './InvitationCodeService';
import { T } from '@tolgee/react';
import React from 'react';

@singleton()
export class SignUpService {
  constructor(
    private http: ApiV1HttpService,
    private tokenService: TokenService,
    private messageService: MessageService,
    private redirectionActions: RedirectionActions,
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
  };

  async verifyEmail(userId: string, code: string) {
    const response = (await this.http.get(
      `public/verify_email/${userId}/${code}`
    )) as TokenDTO;
    this.messageService.success(<T>email_verified_message</T>);
    this.invitationCodeService.disposeCode();
    if (response.accessToken) {
      this.tokenService.setToken(response.accessToken);
      this.globalActions.setJWTToken.dispatch(response.accessToken);
    }
  }
}
