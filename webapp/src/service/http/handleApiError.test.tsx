import { beforeEach, describe, expect, it, vi } from 'vitest';
import { globalContext } from 'tg.globalContext/globalActions';
import { handleApiError } from './handleApiError';

vi.mock('tg.service/MessageService', () => ({
  messageService: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@sentry/browser', () => ({ captureException: vi.fn() }));

describe('navigating away from a response the page cannot use', () => {
  const redirectTo = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    globalContext.actions = { redirectTo } as any;
  });

  const respond = (status: number) => ({ status } as Response);

  it.each(['get', 'GET', undefined])(
    'redirects away from a forbidden GET sent as %s',
    (method) => {
      handleApiError(
        respond(403),
        { code: 'operation_not_permitted' },
        { method },
        {}
      );

      expect(redirectTo).toHaveBeenCalledTimes(1);
    }
  );

  it.each(['get', 'GET', undefined])(
    'redirects away from a missing GET sent as %s',
    (method) => {
      handleApiError(respond(404), {}, { method }, {});

      expect(redirectTo).toHaveBeenCalledTimes(1);
    }
  );

  it.each(['patch', 'PATCH', 'post', 'POST'])(
    'leaves the page alone when a %s is refused',
    (method) => {
      handleApiError(
        respond(403),
        { code: 'operation_not_permitted' },
        { method },
        {}
      );

      expect(redirectTo).not.toHaveBeenCalled();
    }
  );
});
