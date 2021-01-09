import {singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {TokenDTO} from './response.types';
import {tokenService} from './tokenService';
import {messageService} from './messageService';
import {SignUpType} from '../component/security/SignUpView';
import {RedirectionActions} from '../store/global/redirectionActions';
import {GlobalActions} from '../store/global/globalActions';
import {invitationCodeService} from "./invitationCodeService";
import {T} from "@polygloat/react";
import React from "react";

@singleton()
export class signUpService {
    constructor(private http: ApiHttpService, private tokenService: tokenService,
                private messageService: messageService,
                private redirectionActions: RedirectionActions,
                private globalActions: GlobalActions,
                private invitationCodeService: invitationCodeService) {
    }

    validateEmail = async (email: string): Promise<boolean> => {
        return this.http.post('public/validate_email', email);
    };

    signUp = async (data: SignUpType): Promise<void> => {
        const request = {...data, invitationCode: this.invitationCodeService.getCode()} as SignUpType;
        delete request.passwordRepeat;
        let response = await this.http.post('public/sign_up', request) as TokenDTO;
        if (response.accessToken) {
            this.messageService.success(<T>Thanks for your sign up!</T>);
            this.invitationCodeService.disposeCode();
            this.tokenService.setToken(response.accessToken);
            this.globalActions.setJWTToken.dispatch(response.accessToken);
        }
    };

    async verifyEmail(userId: string, code: string) {
        let response = await this.http.get(`public/verify_email/${userId}/${code}`) as TokenDTO;
        this.messageService.success(<T>Thanks for your sign up!</T>);
        this.invitationCodeService.disposeCode();
        if (response.accessToken) {
            this.tokenService.setToken(response.accessToken);
            this.globalActions.setJWTToken.dispatch(response.accessToken);
        }
    }
}
