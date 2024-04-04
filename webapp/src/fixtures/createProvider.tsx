import React, { RefObject, useRef } from 'react';
import { createContext, useContextSelector } from 'use-context-selector';

type SelectorType<StateType, ReturnType> = (state: StateType) => ReturnType;

const createStableActions = (actions: RefObject<any>, stableActions: any) => {
  if (actions.current && !stableActions) {
    const result = {};
    Object.keys(actions.current).map((key) => {
      result[key] = (...args) =>
        (actions.current?.[key] as CallableFunction)?.(...args);
    });
    return result;
  } else {
    return stableActions;
  }
};

export const createProvider = <StateType, Actions, ProviderProps>(
  controller: (
    props: ProviderProps
  ) => [state: StateType, actions: Actions] | undefined | null | JSX.Element
) => {
  const StateContext = createContext<StateType>(null as any);
  const DispatchContext = React.createContext<Actions>(null as any);

  const Provider: React.FC<ProviderProps> = ({ children, ...props }) => {
    const result = controller(props as any);
    const resultIsArray = Array.isArray(result);

    const [state, _actions] = resultIsArray ? result : [];
    const actionsRef = useRef(_actions as Actions | undefined);
    const stableActionsRef = useRef(undefined as Actions | undefined);

    if (!resultIsArray) {
      return <>{result}</>;
    }

    actionsRef.current = _actions;

    // stable actions
    stableActionsRef.current = createStableActions(
      actionsRef,
      stableActionsRef.current
    );

    return (
      <StateContext.Provider value={state as StateType}>
        <DispatchContext.Provider value={stableActionsRef.current!}>
          {children}
        </DispatchContext.Provider>
      </StateContext.Provider>
    );
  };

  const useActions = () => React.useContext(DispatchContext);
  const useStateContext = function <SelectorReturn>(
    selector: SelectorType<StateType, SelectorReturn>
  ) {
    return useContextSelector(StateContext, selector);
  };

  return [Provider, useActions, useStateContext] as const;
};
