import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { T } from '@tolgee/react';
import { IconButton, Tooltip } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { Edit02 } from '@untitled-ui/icons-react';

export const SubscriptionsCloudEditPlanButton: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ item }) => {
  const planId = item.cloudSubscription?.plan.id;
  if (!planId) {
    return null;
  }

  const disabled =
    item.cloudSubscription?.plan.exclusiveForOrganizationId !=
    item.organization.id;

  const tooltipTitle = disabled ? (
    <T keyName="admin-billing-cannot-edit-plan-tooltip" />
  ) : (
    <T keyName="admin_bulling_edit_plan_button" />
  );

  return (
    <Tooltip title={tooltipTitle}>
      <span>
        <IconButton
          sx={{ ml: 1 }}
          size="small"
          disabled={disabled}
          component={Link}
          to={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.build({
            [PARAMS.PLAN_ID]: planId,
          })}
        >
          <Edit02 />
        </IconButton>
      </span>
    </Tooltip>
  );
};
