import React from 'react';
import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { UseQueryResult } from 'react-query';
import { HateoasListData } from 'tg.service/response.types';

type SubscriptionsPopoverCustomPlansProps<T> = {
  getLoadable: () => UseQueryResult<HateoasListData<T>, any>;
  renderItem: (item: T) => React.ReactNode;
};

export const SubscriptionsPopoverCustomPlans = <T,>(
  props: SubscriptionsPopoverCustomPlansProps<T>
) => {
  const loadable = props.getLoadable();

  return (
    <Box sx={{ mt: 3 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <Typography variant="h4" sx={{ fontSize: 16, fontWeight: 'bold' }}>
          <T keyName="admin_billing_organization_custom_plans" />
        </Typography>
      </Box>
      <PaginatedHateoasList
        emptyPlaceholder={
          <Box sx={{ mt: 1, fontStyle: 'italic', opacity: 0.8 }}>
            <T keyName="admin_billing_no_custom_plans" />
          </Box>
        }
        wrapperComponent={Box}
        wrapperComponentProps={{ sx: { mt: 1 } }}
        renderItem={props.renderItem}
        loadable={loadable}
      />
    </Box>
  );
};
