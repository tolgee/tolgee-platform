import { FunctionComponent } from 'react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

interface FullPageLoadingProps {}

export const FullPageLoading: FunctionComponent<FullPageLoadingProps> = () => {
  useGlobalLoading(true);
  return null;
};
