import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StyledActionArea } from 'tg.views/organizations/billing/BillingSection';

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
