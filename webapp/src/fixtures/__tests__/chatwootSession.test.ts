import {
  applyChatwootSession,
  chatwootSessionAction,
  openChatwootSession,
} from 'tg.fixtures/chatwootSession';

const freshSdk = () => {
  const calls: string[] = [];
  return {
    calls,
    sdk: {
      setAttributes: (organization: number) =>
        calls.push(`set:${organization}`),
      reset: () => calls.push('reset'),
    },
  };
};

const step = (
  identifiedUserId: number | undefined,
  {
    currentUserId,
    organization,
    canOpenSupportChat = organization !== undefined,
  }: {
    currentUserId?: number;
    organization?: number;
    canOpenSupportChat?: boolean;
  },
  sdk: ReturnType<typeof freshSdk>['sdk']
) =>
  applyChatwootSession(
    chatwootSessionAction({
      canOpenSupportChat,
      identifiedUserId,
      currentUserId,
    }),
    organization,
    identifiedUserId,
    sdk
  );

describe('applyChatwootSession', () => {
  it('reports nothing when there is no organization to report', () => {
    const { calls, sdk } = freshSdk();

    expect(applyChatwootSession('set-attributes', undefined, 1, sdk)).toBe(1);
    expect(calls).toEqual([]);
  });

  it('reports the current organization onto a session this user opened', () => {
    const { calls, sdk } = freshSdk();
    expect(step(1, { currentUserId: 1, organization: 10 }, sdk)).toBe(1);
    expect(calls).toEqual(['set:10']);
  });

  it('clears the contact a different account left behind, and stops tracking it', () => {
    const { calls, sdk } = freshSdk();
    expect(
      step(2, { currentUserId: 1, organization: 10 }, sdk)
    ).toBeUndefined();
    expect(calls).toEqual(['reset']);
  });

  it('clears the contact on logout', () => {
    const { calls, sdk } = freshSdk();
    expect(
      step(1, { currentUserId: undefined, organization: 10 }, sdk)
    ).toBeUndefined();
    expect(calls).toEqual(['reset']);
  });

  it('leaves a conversation alone on an organization without support chat', () => {
    const { calls, sdk } = freshSdk();
    expect(
      step(
        1,
        { currentUserId: 1, organization: 10, canOpenSupportChat: false },
        sdk
      )
    ).toBe(1);
    expect(calls).toEqual([]);
  });

  it('tells an entitled visitor nothing until they open the chat themselves', () => {
    const { calls, sdk } = freshSdk();
    expect(
      step(undefined, { currentUserId: 1, organization: 10 }, sdk)
    ).toBeUndefined();
    expect(calls).toEqual([]);
  });
});

describe('openChatwootSession', () => {
  const openSdk = () => {
    const calls: string[] = [];
    return {
      calls,
      sdk: {
        reset: () => calls.push('reset'),
        setUser: (user: number) => calls.push(`setUser:${user}`),
        setAttributes: (organization: number) =>
          calls.push(`set:${organization}`),
        toggle: () => calls.push('toggle'),
      },
    };
  };

  it('clears the previous account before identifying this one, and tracks the new one', () => {
    const { calls, sdk } = openSdk();
    expect(
      openChatwootSession(
        'reset',
        { user: 2, userId: 2, organization: 10 },
        sdk
      )
    ).toBe(2);
    expect(calls).toEqual(['reset', 'setUser:2', 'set:10', 'toggle']);
  });

  it('does not clear a session that already belongs to this user', () => {
    const { calls, sdk } = openSdk();
    openChatwootSession(
      'set-attributes',
      { user: 1, userId: 1, organization: 10 },
      sdk
    );
    expect(calls).toEqual(['setUser:1', 'set:10', 'toggle']);
  });

  it('reports the organization the first time the visitor opens the chat themselves', () => {
    const { calls, sdk } = openSdk();
    expect(
      openChatwootSession('none', { user: 1, userId: 1, organization: 10 }, sdk)
    ).toBe(1);
    expect(calls).toEqual(['setUser:1', 'set:10', 'toggle']);
  });

  it('opens without organization attributes when there is none to report', () => {
    const { calls, sdk } = openSdk();
    openChatwootSession(
      'none',
      { user: 1, userId: 1, organization: undefined },
      sdk
    );
    expect(calls).toEqual(['setUser:1', 'toggle']);
  });
});
