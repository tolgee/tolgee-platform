import { ReactNode } from 'react';
import { ErrorResponseDto } from '../service/response.types';
import { AbstractActions } from './AbstractActions';
import { Action, ActionType, PromiseAction, StateModifier } from './Action';

export class LoadableDefinition<
  StateType extends StateWithLoadables<any>,
  PayloadType,
  DispatchParams extends any[]
> {
  constructor(
    public payloadProvider: (...params: DispatchParams) => Promise<any>,
    public then: StateModifier<StateType, PayloadType>,
    public successMessage?: ReactNode,
    public redirectAfter?:
      | string
      | ((action: ActionType<PayloadType>) => string)
  ) {}
}

export abstract class StateWithLoadables<
  ActionsType extends AbstractLoadableActions<any>
> {
  // @ts-ignore
  loadables: {
    [K in keyof ActionsType['loadableDefinitions']]: Loadable<
      Parameters<ActionsType['loadableDefinitions'][K]['then']>[1]['payload'],
      Parameters<ActionsType['loadableDefinitions'][K]['payloadProvider']>
    >;
  } = {} as any;
}

export abstract class AbstractLoadableActions<
  StateType extends StateWithLoadables<any>
> extends AbstractActions<StateType> {
  private _loadableActions;

  get initialState() {
    const loadables = {};
    for (const name in this.loadableDefinitions) {
      loadables[name] = createLoadable();
    }

    return { ...this._initialState, loadables };
  }

  protected constructor(initialState) {
    super(initialState);
  }

  createLoadableDefinition<PayloadType, DispatchParams extends any[]>(
    payloadProvider: (...params: DispatchParams) => Promise<PayloadType>,
    then?: StateModifier<StateType, PayloadType>,
    successMessage?: ReactNode,
    redirectAfter?: string | ((action: ActionType<PayloadType>) => string)
  ) {
    then =
      (then as StateModifier<StateType, PayloadType>) ||
      (((state, action) => state) as StateModifier<StateType, PayloadType>);
    return new LoadableDefinition<StateType, PayloadType, DispatchParams>(
      payloadProvider,
      then,
      successMessage,
      redirectAfter
    );
  }

  private createLoadableAction<PayloadType, DispatchParams extends any[]>(
    loadableName,
    payloadProvider: (...params: DispatchParams) => Promise<any>,
    then?: StateModifier<StateType, PayloadType>,
    successMessage?: ReactNode,
    redirectAfter?: string | ((action: ActionType<PayloadType>) => string)
  ): PromiseAction<PayloadType, ErrorResponseDto, StateType, DispatchParams> {
    return this.createPromiseAction(
      loadableName.toUpperCase(),
      payloadProvider,
      successMessage,
      redirectAfter
    )
      .build.onPending((state, action) => {
        return {
          ...state,
          loadables: {
            ...state.loadables,
            [loadableName]: <Loadable<PayloadType, DispatchParams>>{
              ...state.loadables[loadableName],
              loading: true,
              errorParams: null,
              dispatchParams: action.meta.params,
              touched: true,
            },
          },
        };
      })
      .build.onFullFilled((state, action) => {
        const newState = {
          ...state,
          loadables: {
            ...state.loadables,
            [loadableName]: <Loadable<PayloadType, DispatchParams>>{
              ...state.loadables[loadableName],
              loading: false,
              data: action.payload,
              error: undefined,
              dispatchParams: action.meta.params,
              loaded: true,
            },
          },
        };
        return typeof then === 'function' ? then(newState, action) : newState;
      })
      .build.onRejected((state, action) => {
        //some errors already handled by service layer
        return {
          ...state,
          loadables: {
            ...state.loadables,
            [loadableName]: <Loadable<PayloadType, DispatchParams>>{
              ...state.loadables[loadableName],
              loading: false,
              data: null,
              error: action.payload.__handled ? null : action.payload,
              dispatchParams: action.meta.params,
            },
          },
        };
      });
  }

  public abstract get loadableDefinitions(): {
    [key: string]: LoadableDefinition<StateType, any, any[]>;
  };

  public get loadableActions(): {
    [K in keyof this['loadableDefinitions']]: PromiseAction<
      any,
      any,
      StateType,
      Parameters<this['loadableDefinitions'][K]['payloadProvider']>
    >;
  } {
    if (!this._loadableActions) {
      this._loadableActions = <any>this.generateLoadableActions();
    }
    return this._loadableActions;
  }

  public get loadableReset(): {
    [K in keyof this['loadableDefinitions']]: Action<never, StateType, []>;
  } {
    const loadableResets = {};
    for (const loadableName in this.loadableDefinitions) {
      loadableResets[loadableName] = this.createAction(
        loadableName.toUpperCase() + '_RESET'
      ).build.on((state: StateType) => {
        return this.resetLoadable(state, loadableName);
      });
    }
    return <any>loadableResets;
  }

  protected resetLoadable(
    state: StateType,
    loadableName: keyof this['loadableDefinitions']
  ): StateType {
    return {
      ...state,
      loadables: { ...state.loadables, [loadableName]: createLoadable() },
    };
  }

  private generateLoadableActions() {
    const loadableActions = {};
    for (const loadableName in this.loadableDefinitions) {
      const definition = this.loadableDefinitions[loadableName];
      loadableActions[loadableName] = this.createLoadableAction(
        loadableName,
        definition.payloadProvider,
        definition.then,
        definition.successMessage,
        definition.redirectAfter
      );
    }
    return loadableActions;
  }

  protected createDeleteDefinition = (
    listLoadableName: string,
    payloadProvider: (id: number) => Promise<number>,
    then?: (state: StateType, action: ActionType<number>) => StateType
  ) =>
    this.createLoadableDefinition(payloadProvider, (state, action) => {
      const data = [
        ...(state.loadables[listLoadableName].data as { id: number }[]),
      ];
      const index = data.findIndex((i) => i.id === action.payload);
      if (index > -1) {
        data.splice(index, 1);
      }
      //set new data to list loadable
      state = {
        ...state,
        loadables: {
          ...state.loadables,
          [listLoadableName]: { ...state.loadables[listLoadableName], data },
        },
      };
      return typeof then === 'function' ? then(state, action) : state;
    });

  useSelector<T>(selector: (state: StateType) => T): T {
    throw new Error(
      'useSelector not implemented. Implement it in you Actions class.'
    );
  }
}

export interface Loadable<DataType = any, DispatchParams = any> {
  __discriminator: 'loadable';
  data: DataType | null;
  dispatchParams: DispatchParams | null;
  loading: boolean;
  error?: ErrorResponseDto;
  loaded: boolean;
  touched: boolean;
}

export const createLoadable = <DataType, DispatchParams>(): Loadable<
  DataType,
  DispatchParams
> => ({
  __discriminator: 'loadable',
  data: null,
  loading: false,
  dispatchParams: null,
  loaded: false,
  touched: false,
});
