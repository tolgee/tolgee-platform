import { T } from '@tolgee/react';

import { API_LINKS } from '../constants/apiLinks';
import { LINKS, PARAMS } from '../constants/links';
import { InvitationCodeService } from './InvitationCodeService';
import { messageService } from './MessageService';
import { tokenService } from './TokenService';
import { apiSchemaHttpService } from './http/ApiSchemaHttpService';
import { apiV1HttpService } from './http/ApiV1HttpService';
import { ErrorResponseDto, TokenDTO } from './response.types';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

const API_URL = import.meta.env.VITE_APP_API_URL + '/api/';

interface ResetPasswordPostRequest {
  email: string;
  code: string;
  password: string;
}

export class SecurityService {
  public authorizeOAuthLogin = async (
    type: string,
    code: string
  ): Promise<TokenDTO> => {
    const invitationCode = InvitationCodeService.getCode();
    const invitationCodeQueryPart = invitationCode
      ? '&invitationCode=' + invitationCode
      : '';
    const redirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
      [PARAMS.SERVICE_TYPE]: type,
    });
    const response = await fetch(
      `${API_URL}public/authorize_oauth/${type}?code=${code}&redirect_uri=${redirectUri}${invitationCodeQueryPart}`
    );
    InvitationCodeService.disposeCode();
    return this.handleLoginResponse(response);
  };

  logout() {
    this.removeAfterLoginLink();
    tokenService.disposeToken();
    tokenService.disposeAdminToken();
  }

  async login(v: { username: string; password: string }): Promise<TokenDTO> {
    const response = await fetch(`${API_URL}public/generatetoken`, {
      body: JSON.stringify(v),
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    return this.handleLoginResponse(response);
  }

  setToken(token: string) {
    tokenService.setToken(token);
  }

  public resetPasswordRequest = (email: string) => {
    const url = `${API_LINKS.RESET_PASSWORD_REQUEST}`;
    return apiV1HttpService.post<never>(url, {
      email: email,
      callbackUrl: LINKS.RESET_PASSWORD.buildWithOrigin(),
    });
  };

  public resetPasswordValidate = (email: string, code: string) => {
    const url = `${API_LINKS.RESET_PASSWORD_VALIDATE}/${encodeURIComponent(
      email
    )}/${encodeURIComponent(code)}`;
    return apiV1HttpService.get<never>(url);
  };

  public resetPasswordSet = async (
    email: string,
    code: string,
    password: string
  ): Promise<void> => {
    const url = `${API_LINKS.RESET_PASSWORD_SET}`;
    const res = await apiV1HttpService.post<never>(url, {
      email,
      code,
      password,
    } as ResetPasswordPostRequest);
    messageService.success(<T keyName="password_reset_message" />);
    return res;
  };

  public setLogoutMark = () => {
    localStorage.setItem('logoutMark', 'true');
  };

  public isLogoutMark = () => {
    return !!localStorage.getItem('logoutMark');
  };

  public disposeLogoutMark = () => {
    localStorage.removeItem('logoutMark');
  };

  public saveAfterLoginLink = (afterLoginLink: Record<string, unknown>) => {
    if (!this.isLogoutMark()) {
      localStorage.setItem('afterLoginLink', JSON.stringify(afterLoginLink));
    }
    this.disposeLogoutMark();
  };

  public getAfterLoginLink = (): Record<string, unknown> | null => {
    const link = localStorage.getItem('afterLoginLink');
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
      throw (await response.json()) as ErrorResponseDto;
    }

    const tokenDTO: TokenDTO = await response.json();

    tokenService.setToken(tokenDTO.accessToken);

    const code = InvitationCodeService.getCode();
    if (code) {
      try {
        await apiSchemaHttpService.schemaRequest(
          '/v2/invitations/{code}/accept',
          'get'
        )({ path: { code } });
      } catch (e: any) {
        if (e.code === 'invitation_code_does_not_exist_or_expired') {
          messageService.error(<TranslatedError code={e.code} />);
        } else {
          throw e;
        }
      }
      InvitationCodeService.disposeCode();
    }
    return tokenDTO;
  }
}

export const securityService = new SecurityService();
