import { beforeEach, describe, expect, it, vi } from 'vitest';

const connect = vi.fn();
const disconnect = vi.fn();

vi.mock('@stomp/stompjs', () => ({
  Stomp: {
    over: () => ({
      connect,
      disconnect,
      configure: vi.fn(),
      subscribe: vi.fn(() => ({ unsubscribe: vi.fn(), id: 'sub-1' })),
    }),
  },
}));
vi.mock('sockjs-client/dist/sockjs', () => ({ default: vi.fn() }));

import { WebsocketClient } from 'tg.websocket-client/WebsocketClient';

const CHANNEL = '/projects/1/translation-data-modified' as const;

const sentHeaders = () => connect.mock.calls[0][0];
const fireErrorFrame = (frame?: unknown) => connect.mock.calls[0][2](frame);
const fireConnected = () => connect.mock.calls[0][1]();
const fireDisconnected = () => connect.mock.calls[0][3]();

describe('WebsocketClient', () => {
  beforeEach(() => {
    connect.mockClear();
    disconnect.mockClear();
  });

  it('sends the token as a bearer credential', () => {
    WebsocketClient({ authentication: { jwtToken: 'the-token' } }).subscribe(
      CHANNEL,
      () => {}
    );

    expect(sentHeaders()).toEqual({ Authorization: 'Bearer the-token' });
  });

  it('sends no credential at all when there is no token', () => {
    WebsocketClient({ authentication: { jwtToken: undefined } }).subscribe(
      CHANNEL,
      () => {}
    );

    expect(sentHeaders()).toEqual({});
  });

  it('reports an ERROR frame carrying the Unauthenticated header as unauthenticated', () => {
    const onError = vi.fn();
    WebsocketClient({
      authentication: { jwtToken: 'the-token' },
      onError,
    }).subscribe(CHANNEL, () => {});

    fireErrorFrame({ headers: { message: 'Unauthenticated' } });

    expect(onError).toHaveBeenCalledWith(true);
  });

  it('stops the connection when deactivated, so it cannot redial', () => {
    const client = WebsocketClient({
      authentication: { jwtToken: 'the-token' },
    });
    client.subscribe(CHANNEL, () => {});
    // Drive the client back to idle, or the next subscribe short-circuits on `connecting` instead of
    // on the deactivated guard this case is about.
    fireConnected();
    fireDisconnected();
    connect.mockClear();

    client.deactivate();
    client.subscribe(CHANNEL, () => {});

    expect(disconnect).toHaveBeenCalled();
    expect(connect).not.toHaveBeenCalled();
  });

  it.each([
    ['another header', { headers: { message: 'Internal error' } }],
    ['no headers', {}],
    ['a bare string', 'lost connection'],
    ['nothing', undefined],
  ])('does not report %s as unauthenticated', (_name, frame) => {
    const onError = vi.fn();
    WebsocketClient({
      authentication: { jwtToken: 'the-token' },
      onError,
    }).subscribe(CHANNEL, () => {});

    fireErrorFrame(frame);

    expect(onError).toHaveBeenCalledWith(false);
  });
});
