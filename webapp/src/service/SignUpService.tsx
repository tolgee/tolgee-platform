import { singleton } from 'tsyringe';
import { GlobalActions } from '../store/global/GlobalActions';
import { InvitationCodeService } from './InvitationCodeService';
import { MessageService } from './MessageService';
import { TokenService } from './TokenService';
import { ApiV1HttpService } from './http/ApiV1HttpService';

@singleton()
export class SignUpService {
  constructor(
    private http: ApiV1HttpService,
    private tokenService: TokenService,
    private messageService: MessageService,
    private globalActions: GlobalActions
  ) {}

  validateEmail = async (email: string): Promise<boolean> => {
    return this.http.post('public/validate_email', email);
  };

  async verifyEmail(accessToken: string | undefined) {
    InvitationCodeService.disposeCode();
    if (accessToken) {
      this.tokenService.setToken(accessToken);
      this.globalActions.setJWTToken.dispatch(accessToken);
    }
  }
}
