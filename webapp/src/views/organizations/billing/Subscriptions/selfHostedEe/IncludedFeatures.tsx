import { FC, ReactNode } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { Box, List, ListItem } from '@mui/material';

export const IncludedFeatures: FC<{
  features: components['schemas']['SelfHostedEePlanModel']['enabledFeatures'];
}> = ({ features }) => {
  const { t } = useTranslate();

  const featuresMap: Record<
    components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][0],
    ReactNode
  > = {
    GRANULAR_PERMISSIONS: t(
      'billing_subscriptions_granular_permissions_feature'
    ),
  };

  return (
    <Box>
      <Box mt={1}>
        <T keyName="billing_subscriptions_plan_includes_title" />
      </Box>
      <List>
        {features.map((feature) => (
          <ListItem key={feature}>{featuresMap[feature]}</ListItem>
        ))}
      </List>
    </Box>
  );
};
