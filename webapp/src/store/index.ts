import {applyMiddleware, combineReducers, createStore} from 'redux';
import thunkMiddleware from 'redux-thunk';
import {composeWithDevTools} from 'redux-devtools-extension';
import promise from 'redux-promise-middleware';
import {container} from 'tsyringe';
import {ImplicitReducer as ir} from './ImplicitReducer';
import {RepositoryActions} from './repository/RepositoryActions';
import {LanguageActions} from './languages/LanguageActions';
import {GlobalActions} from './global/GlobalActions';
import {ErrorActions} from './global/ErrorActions';
import {RedirectionActions} from './global/RedirectionActions';
import {MessageActions} from './global/MessageActions';
import {SignUpActions} from './global/SignUpActions';
import {RepositoryInvitationActions} from './repository/invitations/RepositoryInvitationActions';
import {RepositoryPermissionActions} from './repository/invitations/RepositoryPermissionActions';
import {SecurityService} from "../service/SecurityService";
import {MessageService} from "../service/MessageService";
import {TranslationActions} from "./repository/TranslationActions";
import {UserApiKeysActions} from "./api_keys/UserApiKeysActions";
import {ImportExportActions} from "./repository/ImportExportActions";
import {UserActions} from "./global/UserActions";
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
        container.resolve(SecurityService).setLogoutMark();
    }

    return appReducer(state, action);
};

const successMessageMiddleware = store => next => action => {
    if (action.meta && action.meta.successMessage && action.type.indexOf("_PENDING") <= -1 && action.type.indexOf("_REJECTED") <= -1) {
        container.resolve(MessageService).success(action.meta.successMessage);
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
