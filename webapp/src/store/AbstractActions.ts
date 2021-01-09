import {AbstractAction, Action, ActionType, PromiseAction} from './Action';
import {ErrorResponseDTO} from '../service/response.types';
import {Link} from "../constants/links";
import {ReactNode} from "react";

export abstract class AbstractActions<StateType> {
    private actions = new Map<string, AbstractAction>();

    protected readonly _initialState: StateType = null;

    public get initialState(): StateType {
        return {...this._initialState};
    }

    abstract get prefix(): string;


    constructor(initialState: StateType) {
        this._initialState = initialState;
    }

    createAction<PayloadType, DispatchParams extends any[]>(type: string, payloadProvider?: (...params: DispatchParams) => PayloadType):
        Action<PayloadType, StateType, DispatchParams> {
        let action = new Action<PayloadType, StateType, DispatchParams>(`${this.prefix}_${type}`, payloadProvider);
        this.register(action);
        return action;
    }

    createPromiseAction<PayloadType, ErrorType = ErrorResponseDTO, DispatchParams extends any[] = []>(type: string,
                                                                   payloadProvider: (...params: DispatchParams) => Promise<PayloadType>,
                                                                   successMessage?: ReactNode,
                                                                   redirectAfter?: Link):
        PromiseAction<PayloadType, ErrorType, StateType, DispatchParams> {
        let promiseAction = new PromiseAction<PayloadType, ErrorType, StateType, DispatchParams>(`${this.prefix}_${type}`,
            payloadProvider,
            {
                successMessage,
                redirectAfter
            });
        this.register(promiseAction);
        return promiseAction;
    }

    public getAction(type: string): AbstractAction {
        return this.actions.get(type);
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

    public customReducer(state: StateType, action: ActionType<any>, appState): StateType {
        return state;
    }
}
