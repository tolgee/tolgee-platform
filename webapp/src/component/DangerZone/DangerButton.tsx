import { styled } from '@mui/material';
import { ComponentProps } from 'react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledLoadingButton = styled(LoadingButton)`
  white-space: nowrap;
  flex-shrink: 0;
`;

type Props = ComponentProps<typeof LoadingButton>;

export const DangerButton: React.FC<Props> = (props) => {
  return <StyledLoadingButton variant="outlined" color="error" {...props} />;
};
