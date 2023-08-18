import { CircularProgress as MuiCircularProgress } from '@mui/material';
import { useLoadingRegister } from './GlobalLoading';

type Props = React.ComponentProps<typeof MuiCircularProgress>;

export const CircularProgress = (props: Props) => {
  useLoadingRegister(true);
  return <MuiCircularProgress {...props} />;
};
