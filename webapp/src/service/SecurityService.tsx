import {singleton} from 'tsyringe';
import {ApiHttpService} from './ApiHttpService';
import {ErrorResponseDTO, TokenDTO} from './response.types';
import {TokenService} from './TokenService';
import {API_LINKS} from '../constants/apiLinks';
import {LINKS} from '../constants/links';
import {MessageService} from './MessageService';
import {RedirectionActions} from '../store/global/RedirectionActions';
import {InvitationCodeService} from "./InvitationCodeService";
import {InvitationService} from "./InvitationService";
import React from "react";
import {T} from "@tolgee/react";

const API_URL = process.env.REACT_APP_API_URL;

interface ResetPasswordPostRequest {
    email: string,
    code: string,
    password: string,
}

@singleton()
export class SecurityService {
    constructor(private http: ApiHttpService, private tokenService: TokenService, private messageService: MessageService,
                private redirectionActions: RedirectionActions,
                private invitationCodeService: InvitationCodeService,
                private invitationService: InvitationService) {
    }

    public authorizeOAuthLogin = async (type: string, code: string): Promise<TokenDTO> => {
        const invitationCode = this.invitationCodeService.getCode();
        const invitationCodeQueryPart = invitationCode ? "?invitationCode=" + invitationCode : "";
        let response = await fetch(`${API_URL}public/authorize_oauth/${type}/${code}${invitationCodeQueryPart}`);
        this.invitationCodeService.disposeCode();
        return this.handleLoginResponse(response);
    };

    logout() {
        this.removeAfterLoginLink();
        this.tokenService.disposeToken();
    }

    async login(v: { username: string, password: string }): Promise<TokenDTO> {
        const response = await fetch(`${API_URL}public/generatetoken`, {
            body: JSON.stringify(v),
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
        });

        return this.handleLoginResponse(response);
    }

    public resetPasswordRequest = (email: string) => {
        const url = `${API_LINKS.RESET_PASSWORD_REQUEST}`;
        return this.http.post<never>(url, {email: email, callbackUrl: LINKS.RESET_PASSWORD.buildWithOrigin()});
    };

    public resetPasswordValidate = (email: string, code: string) => {
        const url = `${API_LINKS.RESET_PASSWORD_VALIDATE}/${encodeURIComponent(email)}/${encodeURIComponent(code)}`;
        return this.http.get<never>(url);
    };

    public resetPasswordSet = async (email: string, code: string, password: string): Promise<void> => {
        const url = `${API_LINKS.RESET_PASSWORD_SET}`;
        const res = await this.http.post<never>(url, {
            email, code, password
        } as ResetPasswordPostRequest);
        this.messageService.success(<T>Password successfully reset</T>);
        return res;
    };

    public setLogoutMark = () => {
        localStorage.setItem('logoutMark', "true");
    };

    public isLogoutMark = () => {
        return !!localStorage.getItem('logoutMark');
    };

    public disposeLogoutMark = () => {
        localStorage.removeItem('logoutMark');
    };

    public saveAfterLoginLink = (afterLoginLink: object) => {
        if (!this.isLogoutMark()) {
            localStorage.setItem('afterLoginLink', JSON.stringify(afterLoginLink));
        }
        this.disposeLogoutMark();
    };

    public getAfterLoginLink = (): object | null => {
        let link = localStorage.getItem('afterLoginLink');
        if (link) {
            return JSON.parse(link);
        }
        return null;
    };

    public removeAfterLoginLink = () => {
        return localStorage.removeItem('afterLoginLink');
    };

    private async handleLoginResponse(response): Promise<TokenDTO> {
        if (response.status >= 400) {
            throw await response.json() as ErrorResponseDTO;
        }

        const tokenDTO: TokenDTO = await response.json();

        this.tokenService.setToken(tokenDTO.accessToken);

        const code = this.invitationCodeService.getCode();
        if (code) {
            await this.invitationService.acceptInvitation(code);
            this.invitationCodeService.disposeCode();
        }
        return tokenDTO;
    }

}
