import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { ErrorResponseDto, TokenDTO } from 'tg.service/response.types';
import { SecurityService } from 'tg.service/SecurityService';
import { container, singleton } from 'tsyringe';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { SecurityDTO } from './types';
import { TokenService } from 'tg.service/TokenService';

const tokenService = container.resolve(TokenService);

export class GlobalState extends StateWithLoadables<GlobalActions> {
  authLoading = false;
  security: SecurityDTO = {
    allowPrivate: !!tokenService.getToken(),
    jwtToken: tokenService.getToken() || null,
    adminJwtToken: tokenService.getAdminToken() || null,
    loginErrorCode: null,
    allowRegistration: false,
  };
  passwordResetSetLoading = false;
  passwordResetSetValidated = false;
  passwordResetSetError = null;
  passwordResetSetSucceed = false;
  confirmationDialog: ConfirmationDialogProps | null = null;
  loading = false;
  requestSuperJwtAfterActions: {
    onSuccess: () => void;
    onCancel: () => void;
  }[] = [];
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

  debugCustomerAccount = this.createAction(
    'DEBUG_CUSTOMER_ACCOUNT',
    (customerJwt: string) => customerJwt
  ).build.on((state, action) => {
    tokenService.setAdminToken(state.security.jwtToken!);
    tokenService.setToken(action.payload);

    return <GlobalState>{
      ...state,
      security: {
        ...state.security,
        adminJwtToken: state.security.jwtToken,
        jwtToken: action.payload,
      },
    };
  });

  requestSuperJwt = this.createAction(
    'REQUEST_SUPER_JWT',
    (
      payload: (typeof GlobalState)['prototype']['requestSuperJwtAfterActions'][0]
    ) => payload
  ).build.on((state, action) => {
    return <GlobalState>{
      ...state,
      requestSuperJwtAfterActions: [
        ...state.requestSuperJwtAfterActions,
        action.payload,
      ],
    };
  });

  cancelSuperJwtRequest = this.createAction(
    'REQUEST_SUPER_JWT_CANCEL',
    () => {}
  ).build.on((state) => {
    return <GlobalState>{
      ...state,
      requestSuperJwtAfterActions: [],
    };
  });

  successSuperJwtRequest = this.createAction(
    'REQUEST_SUPER_JWT_SUCCESS',
    (jwtToken) => jwtToken
  ).build.on((state, action) => {
    tokenService.setToken(action.payload);
    return <GlobalState>{
      ...state,
      security: {
        ...state.security,
        jwtToken: action.payload,
      },
      requestSuperJwtAfterActions: [],
    };
  });

  exitDebugCustomerAccount = this.createAction(
    'EXIT_DEBUG_CUSTOMER_ACCOUNT',
    () => {}
  ).build.on((state, _) => {
    const adminJwtToken = state.security.adminJwtToken;

    tokenService.disposeAdminToken();
    tokenService.setToken(adminJwtToken!);

    return <GlobalState>{
      ...state,
      security: {
        ...state.security,
        adminJwtToken: null,
        jwtToken: adminJwtToken,
      },
    };
  });

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
