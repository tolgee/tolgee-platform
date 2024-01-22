import { globalActions } from '../store/global/GlobalActions';
import { InvitationCodeService } from './InvitationCodeService';
import { tokenService } from './TokenService';
import { apiV1HttpService } from './http/ApiV1HttpService';

export class SignUpService {
  validateEmail = async (email: string): Promise<boolean> => {
    return apiV1HttpService.post('public/validate_email', email);
  };

  async verifyEmail(accessToken: string | undefined) {
    InvitationCodeService.disposeCode();
    if (accessToken) {
      tokenService.setToken(accessToken);
      globalActions.setJWTToken.dispatch(accessToken);
    }
  }
}

export const signUpService = new SignUpService();
