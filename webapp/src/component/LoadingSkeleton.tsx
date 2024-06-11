import { Skeleton, keyframes, styled } from '@mui/material';
import { useLoadingRegister } from './GlobalLoading';

const fadeIn = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`;

export const FadingIn = styled('div')`
  animation: ${fadeIn} 0.5s ease-in-out;
`;

type Props = React.ComponentProps<typeof Skeleton>;

export const LoadingSkeleton = (props: Props) => {
  useLoadingRegister(true);
  return <Skeleton {...props} />;
};

export const LoadingSkeletonFadingIn = (props: Props) => {
  return (
    <FadingIn>
      <LoadingSkeleton {...props} />
    </FadingIn>
  );
};
