import { styled } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledAction = styled('div')`
  grid-area: action;
  justify-self: end;
`;

type Props = React.ComponentProps<typeof LoadingButton>;

export const PlanActionButton: React.FC<Props> = (props) => {
  return (
    <StyledAction>
      <LoadingButton
        data-cy="billing-plan-action-button"
        variant="outlined"
        color="primary"
        size="small"
        {...props}
      />
    </StyledAction>
  );
};
