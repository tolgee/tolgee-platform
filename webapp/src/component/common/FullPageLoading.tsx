import { FunctionComponent } from 'react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

interface FullPageLoadingProps {}

export const FullPageLoading: FunctionComponent<
  React.PropsWithChildren<FullPageLoadingProps>
> = () => {
  useGlobalLoading(true);
  return null;
};
