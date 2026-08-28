export type SwitchProgress = {
  requested: number;
  settled: number;
};

export type SwitchProgressEvent = {
  kind: 'requested' | 'settled';
  request: number;
};

export const noSwitchInProgress = (): SwitchProgress => ({
  requested: 0,
  settled: 0,
});

/** Newest-wins, not a balanced counter: see 'keeps a stale settle from re-opening a finished switch'. */
export const switchProgress = (
  state: SwitchProgress,
  { kind, request }: SwitchProgressEvent
): SwitchProgress =>
  kind === 'requested'
    ? { ...state, requested: Math.max(state.requested, request) }
    : { ...state, settled: Math.max(state.settled, request) };

export const isSwitchInProgress = ({ requested, settled }: SwitchProgress) =>
  settled < requested;
