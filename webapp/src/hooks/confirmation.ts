import { container } from 'tsyringe';

import { ConfirmationDialogProps } from '../component/common/ConfirmationDialog';
import { GlobalActions } from '../store/global/GlobalActions';

export const confirmation = (options: ConfirmationDialogProps = {}) => {
  const globalActions = container.resolve(GlobalActions);
  globalActions.openConfirmation.dispatch({ ...options });
};
