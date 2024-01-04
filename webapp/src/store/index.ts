import { applyMiddleware, combineReducers, createStore } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import promise from 'redux-promise-middleware';
import thunkMiddleware from 'redux-thunk';

import { messageService } from '../service/MessageService';
import { securityService } from '../service/SecurityService';
import { implicitReducer } from './ImplicitReducer';
import { errorActions } from './global/ErrorActions';
import { globalActions } from './global/GlobalActions';
import { messageActions } from './global/MessageActions';
import { redirectionActions } from './global/RedirectionActions';
import { translationActions } from './project/TranslationActions';

const appReducer = (appState, action) =>
  combineReducers({
    translations: implicitReducer.create(translationActions, appState),
    global: implicitReducer.create(globalActions),
    error: implicitReducer.create(errorActions),
    redirection: implicitReducer.create(redirectionActions),
    message: implicitReducer.create(messageActions),
  })(appState, action);

const rootReducer = (state, action): ReturnType<typeof appReducer> => {
  /**
   * reset state on logout
   */
  if (action.type === globalActions.logout.type) {
    state = undefined;
    //remove after login link to avoid buggy behaviour
    securityService.setLogoutMark();
  }

  return appReducer(state, action);
};

const successMessageMiddleware = (store) => (next) => (action) => {
  if (
    action.meta &&
    action.meta.successMessage &&
    action.type.indexOf('_PENDING') <= -1 &&
    action.type.indexOf('_REJECTED') <= -1
  ) {
    messageService.success(action.meta.successMessage);
  }

  next(action);
};

const redirectAfterMiddleware = (store) => (next) => (action) => {
  if (
    action.meta &&
    action.meta.redirectAfter &&
    action.type.indexOf('_PENDING') <= -1 &&
    action.type.indexOf('_REJECTED') <= -1
  ) {
    const path =
      typeof action.meta.redirectAfter === 'function'
        ? action.meta.redirectAfter(action)
        : action.meta.redirectAfter;
    redirectionActions.redirect.dispatch(path);
  }
  next(action);
};

export type AppState = ReturnType<typeof appReducer>;

export default function configureStore() {
  const middlewares = [
    thunkMiddleware,
    promise,
    redirectAfterMiddleware,
    successMessageMiddleware,
  ];
  const middleWareEnhancer = applyMiddleware(...middlewares);

  return createStore(rootReducer, composeWithDevTools(middleWareEnhancer));
}
