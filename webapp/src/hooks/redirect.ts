import { Link, PARAMS } from '../constants/links';
import { redirectionActions } from '../store/global/RedirectionActions';

export function redirect(to: Link, params?: Partial<{ [K in PARAMS]: any }>) {
  redirectionActions.redirect.dispatch(to.build(params));
}
