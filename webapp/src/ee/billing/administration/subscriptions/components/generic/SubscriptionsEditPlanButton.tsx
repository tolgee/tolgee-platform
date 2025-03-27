import React, { FC } from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { Link } from 'react-router-dom';
import { Edit02 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

type SubscriptionsEditPlanButtonProps = {
  /**
   * Whether the plan is exclusive for the current organization
   */
  isExclusive: boolean;
  link: string;
};

export const SubscriptionsEditPlanButton: FC<
  SubscriptionsEditPlanButtonProps
> = ({ isExclusive, link }) => {
  const tooltipTitle = !isExclusive ? (
    <T keyName="admin-billing-cannot-edit-plan-tooltip" />
  ) : (
    <T keyName="admin_bulling_edit_plan_button" />
  );

  return (
    <Tooltip title={tooltipTitle}>
      <span>
        <IconButton
          data-cy={'administration-edit-current-plan-button'}
          sx={{ ml: 1 }}
          size="small"
          disabled={!isExclusive}
          component={Link}
          to={link}
        >
          <Edit02 />
        </IconButton>
      </span>
    </Tooltip>
  );
};
