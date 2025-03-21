import React, { FC } from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { Plus } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

type SubscriptionsAddPlanProps = {
  onClick: () => void;
};

export const SubscriptionsAddPlanButton: FC<SubscriptionsAddPlanProps> = (
  props
) => {
  const { t } = useTranslate();

  return (
    <>
      <Tooltip
        title={t('admin-subscriptions-add-visible-plan')}
        onClick={props.onClick}
      >
        <IconButton
          size="small"
          sx={{ ml: 1 }}
          color="primary"
          onClick={props.onClick}
        >
          <Plus />
        </IconButton>
      </Tooltip>
    </>
  );
};
