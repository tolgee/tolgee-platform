import { GlobalActions } from 'tg.store/global/GlobalActions';
import { container } from 'tsyringe';

export const errorCapture = (r: Response, data: any) => {
  const globalActions = container.resolve(GlobalActions);
  if (r.status === 400 && data.code === 'plan_translation_limit_exceeded') {
    globalActions.triggerPlanLimitError.dispatch();
  }
};
