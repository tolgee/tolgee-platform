import { container } from 'tsyringe';
import { GlobalActions } from '../store/global/GlobalActions';
import { ConfirmationDialogProps } from '../component/common/ConfirmationDialog';

export const confirmation = (options: ConfirmationDialogProps = {}) => {
  const globalActions = container.resolve(GlobalActions);
  globalActions.openConfirmation.dispatch({ ...options });
};
