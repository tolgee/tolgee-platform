import { Link, PARAMS } from '../constants/links';
import { container } from 'tsyringe';
import { RedirectionActions } from '../store/global/RedirectionActions';

export function useRedirect(
  to: Link,
  params?: Partial<{ [K in PARAMS]: any }>
) {
  container.resolve(RedirectionActions).redirect.dispatch(to.build(params));
}
