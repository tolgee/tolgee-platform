import {SecurityDTO} from './types';
import {singleton} from 'tsyringe';
import {remoteConfigService} from '../../service/remoteConfigService';
import {securityService} from '../../service/securityService';
import {ErrorResponseDTO, RemoteConfigurationDTO, TokenDTO} from '../../service/response.types';
import {userService} from "../../service/userService";
import {ConfirmationDialogProps} from "../../component/common/ConfirmationDialog";
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {invitationCodeService} from "../../service/invitationCodeService";

export class GlobalState extends StateWithLoadables<GlobalActions> {
    authLoading: boolean = false;
    security: SecurityDTO = {
        allowPrivate: !!localStorage.getItem('jwtToken'),
        jwtToken: localStorage.getItem('jwtToken') || null,
        loginErrorCode: null,
        allowRegistration: false
    };
    passwordResetSetLoading = false;
    passwordResetSetValidated = false;
    passwordResetSetError = null;
    passwordResetSetSucceed = false;
    confirmationDialog: ConfirmationDialogProps = null;
    sideMenuOpen: boolean = true;
}


@singleton()
export class GlobalActions extends AbstractLoadableActions<GlobalState> {
    constructor(private configService: remoteConfigService,
                private securityService: securityService,
                private userService: userService,
                private invitationCodeService: invitationCodeService) {
        super(new GlobalState());
    }

    oAuthSuccessful = this.buildLoginAction('AUTHORIZE_OAUTH',
        (serviceType, code) => this.securityService.authorizeOAuthLogin(serviceType, code));


    logout = this.createAction('LOGOUT', () => this.securityService.logout()).build.on(
        (state) => (
            {...state, security: <SecurityDTO>{...state.security, jwtToken: null, allowPrivate: false}}
        ));
    login = this.buildLoginAction('LOGIN', v => this.securityService.login(v));

    resetPasswordValidate = this.createPromiseAction<never, ErrorResponseDTO, Parameters<securityService['resetPasswordValidate']>>('RESET_PASSWORD_VALIDATE',
        this.securityService.resetPasswordValidate)
        .build.onPending((state) => {
            return {...state, passwordResetSetLoading: true};
        }).build.onFullFilled((state) => {
            return {...state, passwordResetSetLoading: false, passwordResetSetValidated: true};
        }).build.onRejected((state, action) => {
            return <GlobalState>{
                ...state,
                passwordResetSetValidated: false,
                passwordResetSetError: action.payload.code,
                passwordResetSetLoading: false
            };
        });
    resetPasswordSet = this.createPromiseAction<void, ErrorResponseDTO, Parameters<securityService['resetPasswordSet']>>('RESET_PASSWORD_SET',
        this.securityService.resetPasswordSet)
        .build.onPending((state) => {
            return <GlobalState>{...state, passwordResetSetLoading: true, passwordResetSetSucceed: false};
        }).build.onFullFilled((state) => {
            return <GlobalState>{...state, passwordResetSetSucceed: true};
        }).build.onRejected((state, action) => {
            return <GlobalState>{
                ...state, passwordResetSetError: action.payload.code,
                passwordResetSetSucceed: false,
                passwordResetSetLoading: false
            };
        });

    setJWTToken = this.createAction('SET_JWT', (token: string) => token).build.on(
        (state, action) => {
            return {
                ...state,
                security: {...state.security, allowPrivate: true, jwtToken: action.payload, loginErrorCode: null, allowRegistration: true}
            };
        });

    allowRegistration = this.createAction('ALLOW_REGISTRATION').build.on((state: GlobalState) => {
        return {...state, security: {...state.security, allowRegistration: true}}
    });

    openConfirmation = this.createAction('OPEN_CONFIRMATION',
        (options: ConfirmationDialogProps) => (options))
        .build.on((state, action) =>
            (<GlobalState>{
                ...state,
                confirmationDialog: action.payload
            }));

    closeConfirmation = this.createAction('CLOSE_CONFIRMATION', null)
        .build.on((state) =>
            (<GlobalState>{
                ...state,
                confirmationDialog: {
                    ...state.confirmationDialog,
                    open: false
                }
            }));

    readonly loadableDefinitions = {
        remoteConfig: this.createLoadableDefinition<RemoteConfigurationDTO>(() => this.configService.getConfiguration(),
            (state, action) => {
                let invitationCode = this.invitationCodeService.getCode();
                return {
                    ...state,
                    security: {
                        ...state.security,
                        allowPrivate: !action.payload.authentication || state.security.allowPrivate,
                        allowRegistration: action.payload.allowRegistrations || !!invitationCode //if user has invitation code, registration is allowed
                    }
                };
            }),
        resetPasswordRequest: this.createLoadableDefinition(this.securityService.resetPasswordRequest)
    };

    readonly toggleSideMenu = this.createAction('TOGGLE_SIDEMENU').build.on((state: GlobalState) => ({
        ...state,
        sideMenuOpen: !state.sideMenuOpen
    } as GlobalState));


    get prefix(): string {
        return 'GLOBAL';
    }

    private buildLoginAction<DispatchParams extends any[]>(name: string, payloadProvider: (...params: DispatchParams) => Promise<TokenDTO>) {
        return this.createPromiseAction<TokenDTO, ErrorResponseDTO, DispatchParams>(name, payloadProvider)
            .build.onPending((state) => (
                {...state, authLoading: true}
            ))
            .build.onFullFilled((state, action) => (
                <GlobalState>{
                    ...state,
                    authLoading: false,
                    security: {...state.security, allowPrivate: true, jwtToken: action.payload.accessToken, loginErrorCode: null}
                }
            )).build.onRejected((state, action) => ({
                ...state,
                authLoading: false,
                security: <SecurityDTO>{allowRegistration: state.security.allowRegistration, loginErrorCode: action.payload.code}
            }));
    }
}

