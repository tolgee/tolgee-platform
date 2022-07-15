import { globalDispatchRef } from 'tg.globalContext/globalActions';

export const errorCapture = (code: string) => {
  switch (code) {
    case 'plan_translation_limit_exceeded':
      globalDispatchRef.current?.({ type: 'INCREMENT_PLAN_LIMIT_ERRORS' });
      break;
    case 'out_of_credits':
      globalDispatchRef.current?.({ type: 'INCREMENT_NO_CREDIT_ERRORS' });
      break;
  }
};
