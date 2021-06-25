import { ReactNode } from 'react';
import { ErrorResponseDto } from '../service/response.types';
import { AbstractAction, Action, ActionType, PromiseAction } from './Action';

export abstract class AbstractActions<StateType> {
  private actions = new Map<string, AbstractAction>();

  protected readonly _initialState: StateType;

  public get initialState(): StateType {
    return { ...this._initialState };
  }

  abstract get prefix(): string;

  constructor(initialState: StateType) {
    this._initialState = initialState;
  }

  createAction<PayloadType, DispatchParams extends any[]>(
    type: string,
    payloadProvider?: (...params: DispatchParams) => PayloadType
  ): Action<PayloadType, StateType, DispatchParams> {
    const action = new Action<PayloadType, StateType, DispatchParams>(
      `${this.prefix}_${type}`,
      payloadProvider
    );
    this.register(action);
    return action;
  }

  createPromiseAction<
    PayloadType,
    ErrorType = ErrorResponseDto,
    DispatchParams extends any[] = []
  >(
    type: string,
    payloadProvider: (...params: DispatchParams) => Promise<PayloadType>,
    successMessage?: ReactNode,
    redirectAfter?: string | ((action: ActionType<PayloadType>) => string)
  ): PromiseAction<PayloadType, ErrorType, StateType, DispatchParams> {
    const promiseAction = new PromiseAction<
      PayloadType,
      ErrorType,
      StateType,
      DispatchParams
    >(`${this.prefix}_${type}`, payloadProvider, {
      successMessage,
      redirectAfter,
    });
    this.register(promiseAction);
    return promiseAction;
  }

  public getAction(type: string): AbstractAction {
    return this.actions.get(type) as AbstractAction;
  }

  protected register(action: AbstractAction) {
    if (action instanceof Action) {
      this.actions.set(action.type, action);
    }
    if (action instanceof PromiseAction) {
      this.actions.set(action.pendingType, action);
      this.actions.set(action.fulfilledType, action);
      this.actions.set(action.rejectedType, action);
    }
  }

  public customReducer(
    state: StateType,
    action: ActionType<any>,
    appState
  ): StateType {
    return state;
  }
}
