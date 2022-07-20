import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { ErrorResponseDto, TokenDTO } from 'tg.service/response.types';
import { SecurityService } from 'tg.service/SecurityService';
import { singleton } from 'tsyringe';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { SecurityDTO } from './types';

export class GlobalState extends StateWithLoadables<GlobalActions> {
  authLoading = false;
  security: SecurityDTO = {
    allowPrivate: !!localStorage.getItem('jwtToken'),
    jwtToken: localStorage.getItem('jwtToken') || null,
    loginErrorCode: null,
    allowRegistration: false,
  };
  passwordResetSetLoading = false;
  passwordResetSetValidated = false;
  passwordResetSetError = null;
  passwordResetSetSucceed = false;
  confirmationDialog: ConfirmationDialogProps | null = null;
  loading = false;
}

@singleton()
export class GlobalActions extends AbstractLoadableActions<GlobalState> {
  constructor(private securityService: SecurityService) {
    super(new GlobalState());
  }

  oAuthSuccessful = this.buildLoginAction(
    'AUTHORIZE_OAUTH',
    (serviceType, code) =>
      this.securityService.authorizeOAuthLogin(serviceType, code)
  );

  logout = this.createAction('LOGOUT', () =>
    this.securityService.logout()
  ).build.on((state) => ({
    ...state,
    security: <SecurityDTO>{
      ...state.security,
      jwtToken: undefined,
      allowPrivate: false,
    },
  }));
  login = this.buildLoginAction('LOGIN', (v) => this.securityService.login(v));

  resetPasswordValidate = this.createPromiseAction<
    never,
    ErrorResponseDto,
    Parameters<SecurityService['resetPasswordValidate']>
  >('RESET_PASSWORD_VALIDATE', this.securityService.resetPasswordValidate)
    .build.onPending((state) => {
      return { ...state, passwordResetSetLoading: true };
    })
    .build.onFullFilled((state) => {
      return {
        ...state,
        passwordResetSetLoading: false,
        passwordResetSetValidated: true,
      };
    })
    .build.onRejected((state, action) => {
      return {
        ...state,
        passwordResetSetValidated: false,
        passwordResetSetError: action.payload.code,
        passwordResetSetLoading: false,
      } as unknown as GlobalState;
    });
  resetPasswordSet = this.createPromiseAction<
    void,
    ErrorResponseDto,
    Parameters<SecurityService['resetPasswordSet']>
  >('RESET_PASSWORD_SET', this.securityService.resetPasswordSet)
    .build.onPending((state) => {
      return <GlobalState>{
        ...state,
        passwordResetSetLoading: true,
        passwordResetSetSucceed: false,
      };
    })
    .build.onFullFilled((state) => {
      return <GlobalState>{ ...state, passwordResetSetSucceed: true };
    })
    .build.onRejected((state, action) => {
      return {
        ...state,
        passwordResetSetError: action.payload.code,
        passwordResetSetSucceed: false,
        passwordResetSetLoading: false,
      } as unknown as GlobalState;
    });

  setJWTToken = this.createAction('SET_JWT', (token: string) => token).build.on(
    (state, action) => {
      return {
        ...state,
        security: {
          ...state.security,
          allowPrivate: true,
          jwtToken: action.payload,
          loginErrorCode: null,
          allowRegistration: true,
        },
      };
    }
  );

  allowRegistration = this.createAction('ALLOW_REGISTRATION').build.on(
    (state: GlobalState) => {
      return {
        ...state,
        security: { ...state.security, allowRegistration: true },
      };
    }
  );

  openConfirmation = this.createAction(
    'OPEN_CONFIRMATION',
    (options: ConfirmationDialogProps) => options
  ).build.on(
    (state, action) =>
      <GlobalState>{
        ...state,
        confirmationDialog: action.payload,
      }
  );

  closeConfirmation = this.createAction(
    'CLOSE_CONFIRMATION',
    undefined
  ).build.on(
    (state) =>
      <GlobalState>{
        ...state,
        confirmationDialog: {
          ...state.confirmationDialog,
          open: false,
        },
      }
  );

  updateSecurity = this.createAction(
    'UPDATE_SECURITY',
    (options: Partial<SecurityDTO>) => options
  ).build.on(
    (state, action) =>
      <GlobalState>{
        ...state,
        security: { ...state.security, ...action.payload },
      }
  );

  readonly loadableDefinitions = {
    resetPasswordRequest: this.createLoadableDefinition<GlobalState, any>(
      this.securityService.resetPasswordRequest
    ),
  };

  get prefix(): string {
    return 'GLOBAL';
  }

  private buildLoginAction<DispatchParams extends any[]>(
    name: string,
    payloadProvider: (...params: DispatchParams) => Promise<TokenDTO>
  ) {
    return this.createPromiseAction<TokenDTO, ErrorResponseDto, DispatchParams>(
      name,
      payloadProvider
    )
      .build.onPending((state) => ({ ...state, authLoading: true }))
      .build.onFullFilled(
        (state, action) =>
          <GlobalState>{
            ...state,
            authLoading: false,
            security: {
              ...state.security,
              allowPrivate: true,
              jwtToken: action.payload.accessToken,
              loginErrorCode: null,
            },
          }
      )
      .build.onRejected((state, action) => ({
        ...state,
        authLoading: false,
        security: <SecurityDTO>{
          allowRegistration: state.security.allowRegistration,
          loginErrorCode: action.payload.code,
        },
      }));
  }
}
