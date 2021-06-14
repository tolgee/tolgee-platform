import { container, singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { ErrorResponseDto, InvitationDTO } from './response.types';
import { RedirectionActions } from '../store/global/RedirectionActions';
import { LINKS } from '../constants/links';
import { MessageService } from './MessageService';
import { TokenService } from './TokenService';
import { InvitationCodeService } from './InvitationCodeService';
import { GlobalActions } from '../store/global/GlobalActions';
import React from 'react';
import { T } from '@tolgee/react';

const http = container.resolve(ApiV1HttpService);

@singleton()
export class InvitationService {
  constructor(
    private redirectActions: RedirectionActions,
    private messaging: MessageService,
    private tokenService: TokenService,
    private invitationCodeService: InvitationCodeService
  ) {}

  public generateInvitationCode = async (
    projectId: number,
    type: string
  ): Promise<string> =>
    await http.post('projects/invite', {
      projectId,
      type,
    });

  public acceptInvitation = async (code: string): Promise<void> => {
    if (!this.tokenService.getToken()) {
      this.invitationCodeService.setCode(code);
      //circular dependency
      container.resolve(GlobalActions).allowRegistration.dispatch();
      this.redirectActions.redirect.dispatch(LINKS.LOGIN.build());
      return;
    }

    try {
      await http.get('invitation/accept/' + code);
      this.messaging.success(<T>Invitation successfully accepted</T>);
    } catch (e) {
      if ((e as ErrorResponseDto).code) {
        this.messaging.error(<T>{e.code}</T>);
      }
    }
    this.redirectActions.redirect.dispatch(LINKS.PROJECTS.build());
  };

  public getInvitations = async (projectId): Promise<InvitationDTO[]> =>
    http.get('invitation/list/' + projectId);

  public deleteInvitation = async (invitationId): Promise<void> =>
    http.delete('invitation/' + invitationId);
}
