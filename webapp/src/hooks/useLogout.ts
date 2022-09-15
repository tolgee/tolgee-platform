import { container } from 'tsyringe';
import { GlobalActions } from 'tg.store/global/GlobalActions';

const globalActions = container.resolve(GlobalActions);

export const useLogout = () => () => globalActions.logout.dispatch();
