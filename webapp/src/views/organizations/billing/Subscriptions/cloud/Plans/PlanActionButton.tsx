import { styled } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';

export const ActionArea = styled('div')`
  grid-area: action;
`;

export const StyledActionArea = styled(ActionArea)`
  justify-self: end;
  align-self: end;
  gap: 8px;
  display: flex;
`;

type Props = React.ComponentProps<typeof LoadingButton>;

export const PlanActionButton: React.FC<Props> = (props) => {
  return (
    <StyledActionArea>
      <LoadingButton
        data-cy="billing-plan-action-button"
        variant="outlined"
        color="primary"
        size="small"
        {...props}
      />
    </StyledActionArea>
  );
};
