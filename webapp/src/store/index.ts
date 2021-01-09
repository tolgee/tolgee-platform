import {applyMiddleware, combineReducers, createStore} from 'redux';
import thunkMiddleware from 'redux-thunk';
import {composeWithDevTools} from 'redux-devtools-extension';
import promise from 'redux-promise-middleware';
import {container} from 'tsyringe';
import {implicitReducer as ir} from './implicitReducer';
import {RepositoryActions} from './repository/RepositoryActions';
import {LanguageActions} from './languages/LanguageActions';
import {GlobalActions} from './global/globalActions';
import {ErrorActions} from './global/errorActions';
import {RedirectionActions} from './global/redirectionActions';
import {MessageActions} from './global/messageActions';
import {SignUpActions} from './global/signUpActions';
import {RepositoryInvitationActions} from './repository/invitations/repositoryInvitationActions';
import {RepositoryPermissionActions} from './repository/invitations/repositoryPermissionActions';
import {securityService} from "../service/securityService";
import {messageService} from "../service/messageService";
import {TranslationActions} from "./repository/TranslationActions";
import {UserApiKeysActions} from "./api_keys/UserApiKeysActions";
import {ImportExportActions} from "./repository/ImportExportActions";
import {UserActions} from "./global/userActions";
import {ScreenshotActions} from "./repository/ScreenshotActions";

const implicitReducer = container.resolve(ir);
const repositoryActionsIns = container.resolve(RepositoryActions);
const languageActionsIns = container.resolve(LanguageActions);
const globalActionsIns = container.resolve(GlobalActions);
const errorActionsIns = container.resolve(ErrorActions);
const redirectionActionsIns = container.resolve(RedirectionActions);

const appReducer = (appState, action) => combineReducers({
    translations: implicitReducer.create(container.resolve(TranslationActions), appState),
    global: implicitReducer.create(globalActionsIns),
    repositories: implicitReducer.create(repositoryActionsIns),
    languages: implicitReducer.create(languageActionsIns),
    error: implicitReducer.create(errorActionsIns),
    redirection: implicitReducer.create(redirectionActionsIns),
    message: implicitReducer.create(container.resolve(MessageActions)),
    signUp: implicitReducer.create(container.resolve(SignUpActions)),
    repositoryInvitation: implicitReducer.create(container.resolve(RepositoryInvitationActions)),
    repositoryPermission: implicitReducer.create(container.resolve(RepositoryPermissionActions)),
    importExport: implicitReducer.create(container.resolve(ImportExportActions)),
    userApiKey: implicitReducer.create(container.resolve(UserApiKeysActions)),
    user: implicitReducer.create(container.resolve(UserActions)),
    screenshots:  implicitReducer.create(container.resolve(ScreenshotActions)),
})(appState, action);

const rootReducer = (state, action): ReturnType<typeof appReducer> => {
    /**
     * reset state on logout
     */
    if (action.type === globalActionsIns.logout.type) {
        state = undefined;
        //remove after login link to avoid buggy behaviour
        container.resolve(securityService).setLogoutMark();
    }

    return appReducer(state, action);
};

const successMessageMiddleware = store => next => action => {
    if (action.meta && action.meta.successMessage && action.type.indexOf("_PENDING") <= -1 && action.type.indexOf("_REJECTED") <= -1) {
        container.resolve(messageService).success(action.meta.successMessage);
    }

    next(action);
};

const redirectAfterMiddleware = store => next => action => {
    if (action.meta && action.meta.redirectAfter && action.type.indexOf("_PENDING") <= -1 && action.type.indexOf("_REJECTED") <= -1) {
        redirectionActionsIns.redirect.dispatch(action.meta.redirectAfter);
    }

    next(action);
};

export type AppState = ReturnType<typeof appReducer>;

export default function configureStore() {
    const middlewares = [thunkMiddleware, promise, redirectAfterMiddleware, successMessageMiddleware];
    const middleWareEnhancer = applyMiddleware(...middlewares);

    return createStore(
        rootReducer,
        composeWithDevTools(middleWareEnhancer)
    );
}
