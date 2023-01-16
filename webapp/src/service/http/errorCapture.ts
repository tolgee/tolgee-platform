import { globalContext } from 'tg.globalContext/globalActions';

export const errorCapture = (code: string) => {
  switch (code) {
    case 'plan_translation_limit_exceeded':
      globalContext.actions?.incrementPlanLimitErrors();
      break;
  }
};
