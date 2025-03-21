import * as React from 'react';
import { FC } from 'react';
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';
import { CloudPlanSelector } from '../../../subscriptionPlans/components/planForm/cloud/fields/CloudPlanSelector';
import { components } from 'tg.service/apiSchema.generated';

export interface SubscriptionsAddPlanDialogProps {
  open: boolean;
  onClose: () => void;
  organization?: components['schemas']['SimpleOrganizationModel'];
}

export const SubscriptionsAddPlanDialog: FC<
  SubscriptionsAddPlanDialogProps
> = ({ open, onClose, organization }) => {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>
        <T keyName="admin-subscriptions-add-visible-plan-dialog-title" />
      </DialogTitle>
      <DialogContent>
        <Typography sx={{ fontSize: 12, mb: 2 }}>
          <T
            keyName="admin-subscriptions-add-visible-plan-dialog-description"
            params={{
              name: organization?.name,
              id: organization?.id,
              b: <b />,
            }}
          />
        </Typography>

        <CloudPlanSelector
          filterPublic={false}
          selectProps={{
            label: (
              <T keyName="admin-subscriptions-add-visible-plan-plan-select-label" />
            ),
            fullWidth: true,
          }}
        />
      </DialogContent>
      <DialogActions></DialogActions>
    </Dialog>
  );
};
