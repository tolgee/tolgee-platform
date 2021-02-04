import {container} from 'tsyringe';
import {dispatchService} from '../service/dispatchService';

export type ActionType<PayloadType> = { type: string, payload: PayloadType, meta?: any, params?: any[] };
export type StateModifier<StateType, PayloadType> = (state: StateType, action: ActionType<PayloadType>) => StateType;

export abstract class AbstractAction<PayloadType = any, StateType = any, DispatchParams extends any[] = any[]> {
    protected constructor(public type: string,
                          public payloadProvider?: (...params: any[]) => PayloadType, public meta?: object) {
    }

    dispatch(...params: DispatchParams) {
        container.resolve(dispatchService).dispatch({
            type: this.type,
            meta: {...this.meta, params: params},
            payload: this.payloadProvider && this.payloadProvider(...params),
        });
    }
}

export class Action<PayloadType, StateType, DispatchParams extends any[]> extends AbstractAction<PayloadType, StateType, DispatchParams> {

    build = {
        on: (callback: StateModifier<StateType, PayloadType>): Action<PayloadType, StateType, DispatchParams> => {
            this.stateModifier = callback;
            return this;
        }
    };

    constructor(public type: string,
                public payloadProvider?: (...params: DispatchParams) => PayloadType,
                public stateModifier?: StateModifier<StateType, PayloadType>,
    ) {
        super(type, payloadProvider);
    }

    public reduce(state: StateType, action: ActionType<PayloadType>): void {
        this.stateModifier(state, action);
    }
}

export class PromiseAction<PayloadType, ErrorType, StateType, DispatchParams extends any[]>
    extends AbstractAction<Promise<PayloadType>, StateType, DispatchParams> {

    public reducePending: StateModifier<StateType, any>;

    get fulfilledType() {
        return this.type + '_FULFILLED';
    }

    get pendingType() {
        return this.type + '_PENDING';
    }

    get rejectedType() {
        return this.type + '_REJECTED';
    }

    public reduceRejected: StateModifier<StateType, any>;
    public reduceFulfilled: StateModifier<StateType, PayloadType>;
    build = {
        onPending: (callback: StateModifier<StateType, any>): PromiseAction<PayloadType, ErrorType, StateType, DispatchParams> => {
            this.reducePending = callback;
            return this;
        },

        onRejected: (callback: StateModifier<StateType, ErrorType>): PromiseAction<PayloadType, ErrorType, StateType, DispatchParams> => {
            this.reduceRejected = callback;
            return this;
        },

        onFullFilled: (callback: StateModifier<StateType, PayloadType>): PromiseAction<PayloadType, ErrorType, StateType, DispatchParams> => {
            this.reduceFulfilled = callback;
            return this;
        }
    };

    constructor(type: string, payloadProvider: (...params: any[]) => Promise<PayloadType>, meta?: object) {
        super(type, payloadProvider, meta);
    }
}
