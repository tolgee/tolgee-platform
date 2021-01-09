import {singleton} from 'tsyringe';
import {Action, ActionType, PromiseAction} from './Action';
import {AbstractActions} from './AbstractActions';

@singleton()
export class implicitReducer {
    create = <StateType>(actions: AbstractActions<StateType>, appState?) =>
        (state = actions.initialState, action: ActionType<any>): StateType => {
            let abstractActionDef = actions.getAction(action.type);

            if (abstractActionDef instanceof PromiseAction) {
                if (action.type === abstractActionDef.pendingType && typeof abstractActionDef.reducePending === 'function') {
                    return abstractActionDef.reducePending(state, action);
                }
                if (action.type === abstractActionDef.rejectedType && typeof abstractActionDef.reduceRejected === 'function') {
                    return abstractActionDef.reduceRejected(state, action);
                }
                if (action.type === abstractActionDef.fulfilledType && typeof abstractActionDef.reduceFulfilled === 'function') {
                    return abstractActionDef.reduceFulfilled(state, action);
                }
            } else if (abstractActionDef instanceof Action) {
                if (action.type === abstractActionDef.type && typeof abstractActionDef.stateModifier === 'function') {
                    return abstractActionDef.stateModifier(state, action);
                }
            }

            return actions.customReducer(state, action, appState);
        };
}

