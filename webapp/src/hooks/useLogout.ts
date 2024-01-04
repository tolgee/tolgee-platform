import { globalActions } from 'tg.store/global/GlobalActions';

export const useLogout = () => () => globalActions.logout.dispatch();
