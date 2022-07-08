import { FC, useEffect } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUpgradePlan } from './BillingPlans/useUpgradePlan';
import { useOrganization } from '../useOrganization';

type PrepareUpgradeDialogProps = {
  data: components['schemas']['SubscriptionUpdatePreviewModel'];
  onClose: () => void;
};

const StyledAppliedCreditsBox = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

export const PrepareUpgradeDialog: FC<PrepareUpgradeDialogProps> = (props) => {
  const { onUpgrade, upgradeMutation } = useUpgradePlan();

  const organization = useOrganization();

  useEffect(() => {
    if (upgradeMutation.isSuccess) {
      props.onClose();
    }
    return () => {
      upgradeMutation.reset();
    };
  }, [upgradeMutation.isSuccess]);

  return (
    <Dialog open={true} onClose={props.onClose}>
      <DialogTitle>
        <T keyName="billing_upgrade_preview_dialog_title" />
      </DialogTitle>
      <DialogContent>
        <span>
          <T keyName="billing_upgrade_preview_dialog_info" />
        </span>

        <Box mt={2}>
          {props.data.items.map((item, idx) => (
            <Box key={idx}>
              {item.description} {item.amount} {item.taxRate}
            </Box>
          ))}
        </Box>

        <Box mt={2}>
          <T
            keyName="billing_upgrade_preview_dialog_total"
            parameters={{ total: props.data.total }}
          />
        </Box>

        {props.data.total < 0 && (
          <StyledAppliedCreditsBox>
            <T keyName="billing_upgrade_preview_dialog_applied_credit_message" />
          </StyledAppliedCreditsBox>
        )}

        <Box mt={2}>
          <b>
            <T
              keyName="billing_upgrade_preview_dialog_amount_due_text"
              parameters={{ amountDue: props.data.amountDue }}
            />
          </b>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={props.onClose}>
          <T keyName="global_cancel_button" />
        </Button>
        <LoadingButton
          loading={upgradeMutation.isLoading}
          onClick={() => onUpgrade(organization!.id, props.data.updateToken)}
          color="primary"
          variant="contained"
        >
          <T keyName="billing_upgrade_preview_confirm_button" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
