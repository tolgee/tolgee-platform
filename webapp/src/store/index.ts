import { applyMiddleware, combineReducers, createStore } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import promise from 'redux-promise-middleware';
import thunkMiddleware from 'redux-thunk';
import { container } from 'tsyringe';

import { MessageService } from '../service/MessageService';
import { SecurityService } from '../service/SecurityService';
import { ImplicitReducer } from './ImplicitReducer';
import { ErrorActions } from './global/ErrorActions';
import { GlobalActions } from './global/GlobalActions';
import { MessageActions } from './global/MessageActions';
import { RedirectionActions } from './global/RedirectionActions';
import { SignUpActions } from './global/SignUpActions';
import { TranslationActions } from './project/TranslationActions';

const implicitReducer = container.resolve(ImplicitReducer);
const globalActions = container.resolve(GlobalActions);
const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

const appReducer = (appState, action) =>
  combineReducers({
    translations: implicitReducer.create(
      container.resolve(TranslationActions),
      appState
    ),
    global: implicitReducer.create(globalActions),
    error: implicitReducer.create(errorActions),
    redirection: implicitReducer.create(redirectionActions),
    message: implicitReducer.create(container.resolve(MessageActions)),
    signUp: implicitReducer.create(container.resolve(SignUpActions)),
  })(appState, action);

const rootReducer = (state, action): ReturnType<typeof appReducer> => {
  /**
   * reset state on logout
   */
  if (action.type === globalActions.logout.type) {
    state = undefined;
    //remove after login link to avoid buggy behaviour
    container.resolve(SecurityService).setLogoutMark();
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
    container.resolve(MessageService).success(action.meta.successMessage);
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
