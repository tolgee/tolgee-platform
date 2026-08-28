import {
  limitErrorCounters,
  noLimitErrors,
} from 'tg.fixtures/limitErrorCounters';

const after = (...events: Parameters<typeof limitErrorCounters>[1][]) =>
  events.reduce(limitErrorCounters, noLimitErrors());

describe('limitErrorCounters', () => {
  it('counts each plan and spending limit error', () => {
    expect(after({ kind: 'plan-limit' }, { kind: 'plan-limit' })).toMatchObject(
      {
        planLimitErrors: 2,
        spendingLimitErrors: 0,
      }
    );
    expect(after({ kind: 'spending-limit' })).toMatchObject({
      spendingLimitErrors: 1,
    });
  });

  it('reports a credit error once, however often it recurs', () => {
    expect(
      after(
        { kind: 'credit-plan-limit' },
        { kind: 'credit-plan-limit' },
        { kind: 'credit-spending-limit' },
        { kind: 'credit-spending-limit' }
      )
    ).toMatchObject({ planLimitErrors: 1, spendingLimitErrors: 1 });
  });

  it('drops counts from the organization the viewer just left', () => {
    expect(
      after(
        { kind: 'plan-limit' },
        { kind: 'spending-limit' },
        { kind: 'organization-changed' }
      )
    ).toEqual(noLimitErrors());
  });
});
