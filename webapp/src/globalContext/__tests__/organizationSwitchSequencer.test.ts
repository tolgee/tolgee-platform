import { afterEach, describe, expect, it, vi } from 'vitest';

import {
  ORGANIZATION_SWITCH_DEADLINE_MS,
  createOrganizationSwitchSequencer,
} from 'tg.globalContext/organizationSwitchSequencer';

type Written = { id: number };

const deferred = () => {
  let resolve!: (value: Written) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<Written>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
};

const harness = () => {
  const pending: Record<number, ReturnType<typeof deferred>> = {};
  const applied: number[] = [];
  const written: number[] = [];
  const settled: number[] = [];
  const requested: number[] = [];

  const switchTo = createOrganizationSwitchSequencer<Written>({
    write: (organizationId) => {
      written.push(organizationId);
      pending[organizationId] = deferred();
      return pending[organizationId].promise;
    },
    apply: (data) => applied.push(data.id),
    onRequested: (request) => requested.push(request),
    onSettled: (request) => settled.push(request),
  });

  return {
    switchTo,
    applied,
    written,
    settled,
    requested,
    settle: (organizationId: number) =>
      pending[organizationId].resolve({ id: organizationId }),
    fail: (organizationId: number) =>
      pending[organizationId].reject(new Error('refused')),
  };
};

const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('createOrganizationSwitchSequencer', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('settles every request it announced, including one superseded before its write ran', async () => {
    const h = harness();

    h.switchTo(1, undefined);
    const second = h.switchTo(2, undefined);
    await flush();

    h.settle(2);
    expect(await second).toBe(true);
    await flush();

    expect([...h.settled].sort()).toEqual([...h.requested].sort());
  });

  it('gives up waiting on a write that never settles, without blocking the UI forever', async () => {
    vi.useFakeTimers();
    const h = harness();
    const first = h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.settled).toEqual([]);

    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS);

    expect(await first).toBe(false);
    expect(h.settled).toEqual([1]);
    expect(h.applied).toEqual([]);
  });

  it('still issues a later switch after an earlier write blew its deadline', async () => {
    vi.useFakeTimers();
    const h = harness();
    const first = h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS);

    expect(await first).toBe(false);

    const second = h.switchTo(2, undefined);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.written).toEqual([1, 2]);

    h.settle(2);
    await vi.advanceTimersByTimeAsync(0);

    expect(await second).toBe(true);
    expect(h.applied).toEqual([2]);

    h.settle(1);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.settled).toEqual([1, 2, 1]);
  });

  it('gives a queued switch its whole budget, not what the blocked predecessor left over', async () => {
    vi.useFakeTimers();
    const h = harness();

    const first = h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);

    await vi.advanceTimersByTimeAsync(1000);
    const second = h.switchTo(2, undefined);

    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS - 1000);
    expect(await first).toBe(false);
    expect(h.written).toEqual([1, 2]);

    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS - 1000);
    h.settle(2);
    await vi.advanceTimersByTimeAsync(0);

    expect(await second).toBe(true);
    expect(h.applied).toEqual([2]);
  });

  it('does not report the previous organization as already-preferred while an abandoned write can still land', async () => {
    vi.useFakeTimers();
    const h = harness();

    const away = h.switchTo(2, 1);
    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS);
    expect(await away).toBe(false);

    h.switchTo(1, 1);
    await vi.advanceTimersByTimeAsync(0);

    h.settle(2);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.applied).toEqual([]);
  });

  it('retries the same organization after its write blew the deadline, instead of handing back the failed promise', async () => {
    vi.useFakeTimers();
    const h = harness();
    const first = h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS);

    expect(await first).toBe(false);

    h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.written).toEqual([1, 1]);
  });

  it('still applies a write that lands after its deadline, so the client cannot end up behind the server', async () => {
    vi.useFakeTimers();
    const h = harness();
    const first = h.switchTo(1, undefined);
    await vi.advanceTimersByTimeAsync(0);
    await vi.advanceTimersByTimeAsync(ORGANIZATION_SWITCH_DEADLINE_MS);

    expect(await first).toBe(false);

    h.settle(1);
    await vi.advanceTimersByTimeAsync(0);

    expect(h.applied).toEqual([1]);
  });

  it('applies the write when nothing supersedes it', async () => {
    const h = harness();
    const switched = h.switchTo(1, undefined);
    await flush();

    h.settle(1);

    expect(await switched).toBe(true);
    expect(h.applied).toEqual([1]);
  });

  it('does not apply a request that a newer one superseded while it was in flight', async () => {
    const h = harness();
    const first = h.switchTo(1, undefined);
    await flush();
    expect(h.written).toEqual([1]);

    const second = h.switchTo(2, undefined);
    h.settle(1);

    expect(await first).toBe(false);
    await flush();
    h.settle(2);
    expect(await second).toBe(true);

    expect(h.applied).toEqual([2]);
  });

  it('never issues the write for a request superseded before the chain reached it', async () => {
    const h = harness();
    const first = h.switchTo(1, undefined);
    await flush();

    const second = h.switchTo(2, undefined);
    const third = h.switchTo(3, undefined);

    h.settle(1);
    await flush();

    expect(await first).toBe(false);
    expect(await second).toBe(false);
    expect(h.written).toEqual([1, 3]);

    h.settle(3);
    expect(await third).toBe(true);
    expect(h.applied).toEqual([3]);
  });

  it('shares the in-flight promise when the same organization is requested again', async () => {
    const h = harness();
    const first = h.switchTo(1, undefined);
    const again = h.switchTo(1, undefined);
    await flush();

    expect(h.written).toEqual([1]);
    expect(h.requested).toEqual([1]);

    h.settle(1);
    expect(await first).toBe(true);
    expect(await again).toBe(true);
  });

  it('skips the write when the organization is already preferred and nothing is in flight', async () => {
    const h = harness();

    expect(await h.switchTo(7, 7)).toBe(true);
    expect(h.written).toEqual([]);
    expect(h.requested).toEqual([]);
  });

  it('settles a failed write without applying it, and lets the next one through', async () => {
    const h = harness();
    const first = h.switchTo(1, undefined);
    await flush();

    h.fail(1);
    expect(await first).toBe(false);
    expect(h.applied).toEqual([]);
    expect(h.settled).toEqual([1]);

    const second = h.switchTo(2, undefined);
    await flush();
    h.settle(2);

    expect(await second).toBe(true);
    expect(h.applied).toEqual([2]);
  });
});
