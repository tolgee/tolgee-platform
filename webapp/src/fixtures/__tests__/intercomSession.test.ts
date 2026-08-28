import {
  applyIntercomSession,
  IntercomTrackers,
  intercomSessionAction,
} from 'tg.fixtures/intercomSession';

const action = (over: Partial<Parameters<typeof intercomSessionAction>[0]>) =>
  intercomSessionAction({
    canOpenSupportChat: false,
    installed: false,
    booted: false,
    identityChanged: false,
    ...over,
  });

describe('intercomSessionAction', () => {
  it('installs the widget the first time support chat is available', () => {
    expect(action({ canOpenSupportChat: true })).toBe('install');
  });

  it('boots a shut-down session rather than re-installing the widget', () => {
    expect(action({ canOpenSupportChat: true, installed: true })).toBe('boot');
  });

  it('updates a live session instead of tearing it down and re-booting', () => {
    expect(
      action({ canOpenSupportChat: true, installed: true, booted: true })
    ).toBe('update');
  });

  it('ends the first session before starting one for a different user', () => {
    expect(
      action({
        canOpenSupportChat: true,
        installed: true,
        booted: true,
        identityChanged: true,
      })
    ).toBe('reboot');
  });

  it('moves a live session to another company without discarding it', () => {
    expect(
      action({
        canOpenSupportChat: true,
        installed: true,
        booted: true,
      })
    ).toBe('update');
  });

  it('shuts a live session down when support chat is withdrawn', () => {
    expect(action({ installed: true, booted: true })).toBe('shutdown');
  });

  it('never boots a viewer without the entitlement, whose identity would then reach Intercom', () => {
    expect(action({ canOpenSupportChat: false })).toBe('none');
    expect(action({ canOpenSupportChat: false, installed: true })).toBe('none');
  });

  it('does nothing when there is no session to shut down', () => {
    expect(action({ installed: true })).toBe('none');
  });
});

type Settings = { company?: number };

const freshSdk = () => {
  const calls: string[] = [];
  return {
    calls,
    sdk: {
      install: () => calls.push('install'),
      boot: () => calls.push('boot'),
      update: () => calls.push('update'),
      shutdown: () => calls.push('shutdown'),
    },
  };
};

const closed: IntercomTrackers = {
  installed: false,
  booted: false,
  bootedFor: undefined,
};

const step = (
  trackers: IntercomTrackers,
  { userId, companyId }: { userId?: number; companyId?: number },
  sdk: ReturnType<typeof freshSdk>['sdk']
) => {
  // Mirrors useIntercom: without a company there is nothing to attribute a session to.
  const canOpenSupportChat = userId !== undefined && companyId !== undefined;
  const action = intercomSessionAction({
    canOpenSupportChat,
    installed: trackers.installed,
    booted: trackers.booted,
    identityChanged: trackers.bootedFor !== userId,
  });
  return applyIntercomSession<Settings>(
    action,
    { settings: () => ({ company: companyId }), userId, companyId },
    trackers,
    sdk
  );
};

describe('applyIntercomSession', () => {
  it('walks a session through install, company switch, company loss, logout and remount', () => {
    const { calls, sdk } = freshSdk();
    let trackers = closed;

    trackers = step(trackers, { userId: 1, companyId: 10 }, sdk);
    expect(calls).toEqual(['install']);
    expect(trackers).toEqual({
      installed: true,
      booted: true,
      bootedFor: 1,
    });

    trackers = step(trackers, { userId: 1, companyId: 20 }, sdk);
    expect(calls).toEqual(['install', 'update']);

    // Losing the company withdraws the entitlement itself, so the session ends rather than reboots.
    trackers = step(trackers, { userId: 1, companyId: undefined }, sdk);
    expect(calls).toEqual(['install', 'update', 'shutdown']);

    trackers = step(trackers, {}, sdk);
    expect(calls.at(-1)).toBe('shutdown');
    expect(trackers).toEqual({
      installed: true,
      booted: false,
      bootedFor: undefined,
      bootedCompany: undefined,
    });

    trackers = step(trackers, { userId: 2, companyId: 30 }, sdk);
    expect(calls.at(-1)).toBe('boot');
    expect(trackers.bootedFor).toBe(2);
  });

  it('ends the previous session before starting one for a different user', () => {
    const { calls, sdk } = freshSdk();
    let trackers = step(closed, { userId: 1, companyId: 10 }, sdk);
    trackers = step(trackers, { userId: 2, companyId: 10 }, sdk);

    expect(calls).toEqual(['install', 'shutdown', 'boot']);
    expect(trackers.bootedFor).toBe(2);
  });

  it('does nothing when there is no session and no entitlement', () => {
    const { calls, sdk } = freshSdk();
    expect(step(closed, {}, sdk)).toEqual(closed);
    expect(calls).toEqual([]);
  });
});
