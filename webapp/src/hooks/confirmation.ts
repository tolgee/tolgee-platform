import { ConfirmationDialogProps } from '../component/common/ConfirmationDialog';
import { globalActions } from '../store/global/GlobalActions';

export const confirmation = (options: ConfirmationDialogProps = {}) => {
  globalActions.openConfirmation.dispatch({ ...options });
};
