import {container, singleton} from 'tsyringe';
import {ApiHttpService} from './ApiHttpService';
import {ErrorResponseDTO, InvitationDTO} from './response.types';
import {RedirectionActions} from '../store/global/RedirectionActions';
import {LINKS} from '../constants/links';
import {MessageService} from './MessageService';
import {TokenService} from "./TokenService";
import {InvitationCodeService} from "./InvitationCodeService";
import {GlobalActions} from "../store/global/GlobalActions";
import React from "react";
import {T} from "@tolgee/react";


const http = container.resolve(ApiHttpService);


@singleton()
export class InvitationService {
    constructor(private redirectActions: RedirectionActions, private messaging: MessageService, private tokenService: TokenService,
                private invitationCodeService: InvitationCodeService) {
    }

    public generateInvitationCode = async (repositoryId: number, type: string): Promise<string> => await http.post('repositories/invite', {
        repositoryId, type
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
            if ((e as ErrorResponseDTO).code) {
                this.messaging.error(<T>e.code</T>);
            }
        }
        this.redirectActions.redirect.dispatch(LINKS.REPOSITORIES.build());
    };

    public getInvitations = async (repositoryId): Promise<InvitationDTO[]> => http.get('invitation/list/' + repositoryId);

    public deleteInvitation = async (invitationId): Promise<void> => http.delete('invitation/' + invitationId);
}