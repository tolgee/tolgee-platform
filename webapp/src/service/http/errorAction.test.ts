import { beforeEach, describe, expect, it, vi } from 'vitest';
import { globalContext } from 'tg.globalContext/globalActions';
import { errorAction } from './errorAction';

describe('limit error routing', () => {
  const incrementPlanLimitErrors = vi.fn();
  const incrementSpendingLimitErrors = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    globalContext.actions = {
      incrementPlanLimitErrors,
      incrementSpendingLimitErrors,
    } as any;
  });

  it.each([
    'plan_translation_limit_exceeded',
    'plan_seat_limit_exceeded',
    'plan_key_limit_exceeded',
    'plan_word_limit_exceeded',
  ])('routes %s to the plan-limit dialog', (code) => {
    expect(errorAction(code)).toBe(true);
    expect(incrementPlanLimitErrors).toHaveBeenCalledWith(code);
    expect(incrementSpendingLimitErrors).not.toHaveBeenCalled();
  });

  it.each([
    'translation_spending_limit_exceeded',
    'seats_spending_limit_exceeded',
    'keys_spending_limit_exceeded',
    'words_spending_limit_exceeded',
  ])('routes %s to the spending-limit dialog', (code) => {
    expect(errorAction(code)).toBe(true);
    expect(incrementSpendingLimitErrors).toHaveBeenCalled();
    expect(incrementPlanLimitErrors).not.toHaveBeenCalled();
  });

  it('leaves an unrelated code to the caller', () => {
    expect(errorAction('key_exists')).toBe(false);
    expect(incrementPlanLimitErrors).not.toHaveBeenCalled();
    expect(incrementSpendingLimitErrors).not.toHaveBeenCalled();
  });
});
