import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { PlanTitleArea, PlanTitleText } from '../common/PlanTitle';
import { Box, Tooltip, Typography } from '@mui/material';
import { SubscriptionStatus } from '../../common/SubscriptionStatus';
import { PlanLicenseKey } from './PlanLicenseKey';
import { T } from '@tolgee/react';

export const ActivePlanTitle: FC<{
  name: string;
  status: components['schemas']['SelfHostedEeSubscriptionModel']['status'];
  licenseKey?: string;
  createdAt?: number;
}> = (props) => {
  const formatDate = useDateFormatter();

  return (
    <PlanTitleArea sx={{ mb: 2 }}>
      <Box>
        <Box sx={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <PlanTitleText>{props.name}</PlanTitleText>
          <Box>
            <SubscriptionStatus status={props.status} />
          </Box>
          <PlanLicenseKey licenseKey={props.licenseKey} />
        </Box>
      </Box>
      {props.createdAt && (
        <Tooltip title={<T keyName="active-plan-subscribed-at-tooltip" />}>
          <Typography variant="caption">
            {formatDate(props.createdAt)}
          </Typography>
        </Tooltip>
      )}
    </PlanTitleArea>
  );
};
