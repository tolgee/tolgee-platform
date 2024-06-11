import { Skeleton } from '@mui/material';
import { useLoadingRegister } from './GlobalLoading';

type Props = React.ComponentProps<typeof Skeleton>;

export const LoadingSkeleton = (props: Props) => {
  useLoadingRegister(true);
  return <Skeleton {...props} />;
};
