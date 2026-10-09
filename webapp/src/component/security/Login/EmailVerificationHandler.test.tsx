import { reportVerificationError } from './EmailVerificationHandler';
import { messageService } from 'tg.service/MessageService';

vi.mock('tg.service/MessageService', () => ({
  messageService: { success: vi.fn() },
}));

describe('reportVerificationError', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('treats an already-verified link as success', () => {
    const handleError = vi.fn();
    reportVerificationError({ code: 'email_already_verified', handleError });

    expect(messageService.success).toHaveBeenCalledTimes(1);
    expect(handleError).not.toHaveBeenCalled();
  });

  it('reports every other error, so an expired link is not silent', () => {
    const handleError = vi.fn();
    reportVerificationError({ code: 'invalid_code', handleError });

    expect(handleError).toHaveBeenCalledTimes(1);
    expect(messageService.success).not.toHaveBeenCalled();
  });

  it('does not throw when the error carries no handler', () => {
    expect(() =>
      reportVerificationError({ code: 'invalid_code' })
    ).not.toThrow();
  });
});
