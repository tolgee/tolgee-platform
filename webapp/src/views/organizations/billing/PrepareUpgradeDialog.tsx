import React, { FC, useEffect } from 'react';
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
import { useUpgradePlan } from 'tg.component/billing/Plan/useUpgradePlan';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';

type PrepareUpgradeDialogProps = {
  data: components['schemas']['SubscriptionUpdatePreviewModel'];
  onClose: () => void;
};

const StyledAppliedCreditsBox = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledItemDescription = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledItemPrice = styled(Box)`
  padding: 4px 0px 16px 16px;
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

  const formatMoney = useMoneyFormatter();
  const formatNumber = useNumberFormatter();

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
            <React.Fragment key={idx}>
              <StyledItemDescription>{item.description}:</StyledItemDescription>
              <StyledItemPrice>
                {formatMoney(item.amount)} (
                {formatMoney(item.amount * (1 + item.taxRate / 100))}
                {' with '}
                {formatNumber(item.taxRate)}% tax)
              </StyledItemPrice>
            </React.Fragment>
          ))}
        </Box>

        <Box mt={2}>
          <T
            keyName="billing_upgrade_preview_dialog_total"
            params={{ total: props.data.total }}
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
              params={{ amountDue: props.data.amountDue }}
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
          data-cy="billing-upgrade-preview-confirm-button"
        >
          <T keyName="billing_upgrade_preview_confirm_button" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
