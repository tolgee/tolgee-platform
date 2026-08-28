export type LimitErrorCounters = {
  planLimitErrors: number;
  spendingLimitErrors: number;
};

export type LimitErrorEvent =
  | { kind: 'plan-limit' }
  | { kind: 'spending-limit' }
  | { kind: 'credit-plan-limit' }
  | { kind: 'credit-spending-limit' }
  | { kind: 'organization-changed' };

export const noLimitErrors = (): LimitErrorCounters => ({
  planLimitErrors: 0,
  spendingLimitErrors: 0,
});

export const limitErrorCounters = (
  state: LimitErrorCounters,
  event: LimitErrorEvent
): LimitErrorCounters => {
  switch (event.kind) {
    case 'plan-limit':
      return { ...state, planLimitErrors: state.planLimitErrors + 1 };
    case 'spending-limit':
      return { ...state, spendingLimitErrors: state.spendingLimitErrors + 1 };
    case 'credit-plan-limit':
      return state.planLimitErrors > 0
        ? state
        : { ...state, planLimitErrors: 1 };
    case 'credit-spending-limit':
      return state.spendingLimitErrors > 0
        ? state
        : { ...state, spendingLimitErrors: 1 };
    case 'organization-changed':
      // The caller's effect is keyed on the organization id, so reaching here IS the change.
      return noLimitErrors();
  }
};
