import {
  isSwitchInProgress,
  noSwitchInProgress,
  switchProgress,
} from 'tg.fixtures/switchProgress';

const after = (...events: Parameters<typeof switchProgress>[1][]) =>
  events.reduce(switchProgress, noSwitchInProgress());

describe('switchProgress', () => {
  it('is switching from the request until its settle arrives', () => {
    expect(isSwitchInProgress(after({ kind: 'requested', request: 1 }))).toBe(
      true
    );
    expect(
      isSwitchInProgress(
        after(
          { kind: 'requested', request: 1 },
          { kind: 'settled', request: 1 }
        )
      )
    ).toBe(false);
  });

  it('stops switching once a stale write settles after a newer one', () => {
    const state = after(
      { kind: 'requested', request: 1 },
      { kind: 'settled', request: 1 },
      { kind: 'requested', request: 2 },
      { kind: 'settled', request: 2 },
      { kind: 'settled', request: 1 }
    );

    expect(state.settled).toBe(2);
    expect(isSwitchInProgress(state)).toBe(false);
  });

  it('keeps switching while a newer request is still outstanding', () => {
    const state = after(
      { kind: 'requested', request: 1 },
      { kind: 'settled', request: 1 },
      { kind: 'requested', request: 2 }
    );

    expect(isSwitchInProgress(state)).toBe(true);
  });
});
