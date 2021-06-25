import { T } from '@tolgee/react';
import { singleton } from 'tsyringe';

import { API_LINKS } from '../constants/apiLinks';
import { LINKS } from '../constants/links';
import { InvitationCodeService } from './InvitationCodeService';
import { MessageService } from './MessageService';
import { TokenService } from './TokenService';
import { ApiSchemaHttpService } from './http/ApiSchemaHttpService';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { ErrorResponseDto, TokenDTO } from './response.types';

const API_URL = process.env.REACT_APP_API_URL + '/api/';

interface ResetPasswordPostRequest {
  email: string;
  code: string;
  password: string;
}

@singleton()
export class SecurityService {
  constructor(
    private http: ApiV1HttpService,
    private tokenService: TokenService,
    private messageService: MessageService,
    private invitationCodeService: InvitationCodeService,
    private apiSchemaService: ApiSchemaHttpService
  ) {}

  public authorizeOAuthLogin = async (
    type: string,
    code: string
  ): Promise<TokenDTO> => {
    const invitationCode = this.invitationCodeService.getCode();
    const invitationCodeQueryPart = invitationCode
      ? '?invitationCode=' + invitationCode
      : '';
    const response = await fetch(
      `${API_URL}public/authorize_oauth/${type}/${code}${invitationCodeQueryPart}`
    );
    this.invitationCodeService.disposeCode();
    return this.handleLoginResponse(response);
  };

  logout() {
    this.removeAfterLoginLink();
    this.tokenService.disposeToken();
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

  public resetPasswordRequest = (email: string) => {
    const url = `${API_LINKS.RESET_PASSWORD_REQUEST}`;
    return this.http.post<never>(url, {
      email: email,
      callbackUrl: LINKS.RESET_PASSWORD.buildWithOrigin(),
    });
  };

  public resetPasswordValidate = (email: string, code: string) => {
    const url = `${API_LINKS.RESET_PASSWORD_VALIDATE}/${encodeURIComponent(
      email
    )}/${encodeURIComponent(code)}`;
    return this.http.get<never>(url);
  };

  public resetPasswordSet = async (
    email: string,
    code: string,
    password: string
  ): Promise<void> => {
    const url = `${API_LINKS.RESET_PASSWORD_SET}`;
    const res = await this.http.post<never>(url, {
      email,
      code,
      password,
    } as ResetPasswordPostRequest);
    this.messageService.success(<T>Password successfully reset</T>);
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

    this.tokenService.setToken(tokenDTO.accessToken);

    const code = this.invitationCodeService.getCode();
    if (code) {
      await this.apiSchemaService.schemaRequest(
        '/api/invitation/accept/{code}',
        'get'
      )({ path: { code } });
      this.invitationCodeService.disposeCode();
    }
    return tokenDTO;
  }
}
