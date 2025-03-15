import { globalContext } from 'tg.globalContext/globalActions';

/**
 * Performs action for defined error codes
 *
 * @return Returns true if the error is handled and other error handling should be skipped
 */
export const errorAction = (code: string) => {
  switch (code) {
    case 'plan_translation_limit_exceeded':
      globalContext.actions?.incrementPlanLimitErrors();
      return true;
    case 'translation_spending_limit_exceeded':
      globalContext.actions?.incrementSpendingLimitErrors();
      return true;
    case 'seats_spending_limit_exceeded':
      globalContext.actions?.incrementSpendingLimitErrors();
      return true;
    case 'plan_seat_limit_exceeded':
      globalContext.actions?.incrementPlanLimitErrors();
      return true;
    case 'keys_spending_limit_exceeded':
      globalContext.actions?.incrementSpendingLimitErrors();
      return true;
    case 'plan_key_limit_exceeded':
      globalContext.actions?.incrementPlanLimitErrors();
      return true;
    default:
      return false;
  }
};
