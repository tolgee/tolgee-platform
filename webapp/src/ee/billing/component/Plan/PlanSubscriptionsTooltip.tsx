import { components } from 'tg.service/billingApiSchema.generated';
import {
  Box,
  Button,
  Chip,
  Link,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { PlanSubscriptionCount } from 'tg.ee.module/billing/component/Plan/PlanSubscriptionCount';
import { UseInfiniteQueryResult } from 'react-query';
import { HateoasListData } from 'tg.service/response.types';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { SubscriptionStatusChip } from './SubscriptionStatusChip';
import { BillingPeriodChip } from './BillingPeriodChip';
import { useTranslate } from '@tolgee/react';

type CloudPlanModel = components['schemas']['AdministrationCloudPlanModel'];
type SubscriptionModel =
  components['schemas']['AdministrationBasicSubscriptionModel'];

export const PlanSubscriptionsTooltip = ({
  plan,
  subscriptions,
  onOpen,
}: {
  plan: CloudPlanModel;
  subscriptions: UseInfiniteQueryResult<HateoasListData<SubscriptionModel>>;
  onOpen?: () => void;
}) => {
  const { t } = useTranslate();
  const isLoading = subscriptions.isLoading;
  const isFetchingNextPage = subscriptions.isFetchingNextPage;
  const subscriptionItems =
    subscriptions.data?.pages.flatMap((page) => page._embedded?.plans || []) ||
    [];

  if (!plan.subscriptionCount) {
    return null;
  }

  const tooltipTitle = (
    <Box display="flex" flexDirection="column" gap={1}>
      {isLoading ? (
        <Box display="flex" justifyContent="center" py={1}>
          <SpinnerProgress size={20} />
        </Box>
      ) : (
        subscriptionItems.length && (
          <Box
            component="ul"
            sx={{
              listStyle: 'none',
              m: 0,
              p: 0,
              display: 'flex',
              flexDirection: 'column',
              gap: 1,
            }}
          >
            {subscriptionItems.map((item, index) => (
              <Box
                component="li"
                key={`${item.organization}-${index}`}
                display="flex"
                justifyContent="space-between"
                gap={1.5}
                alignItems="flex-start"
              >
                <Box minWidth={200}>
                  <Link
                    component={RouterLink}
                    to={LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
                      [PARAMS.ORGANIZATION_SLUG]: item.organizationSlug,
                    })}
                  >
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {item.organization}
                    </Typography>
                  </Link>
                  <Typography variant="caption" color="text.secondary" noWrap>
                    {item.planName}
                  </Typography>
                </Box>
                <Stack
                  direction="row"
                  spacing={0.5}
                  flexWrap="wrap"
                  justifyContent="flex-end"
                  useFlexGap
                  rowGap={0.5}
                  columnGap={0.5}
                >
                  <BillingPeriodChip period={item.currentBillingPeriod} />
                  <SubscriptionStatusChip status={item.status} />
                  {item.cancelAtPeriodEnd && (
                    <Chip
                      size="small"
                      variant="outlined"
                      label={t('billing_subscription_cancelled')}
                    />
                  )}
                </Stack>
              </Box>
            ))}
            {subscriptions.hasNextPage && (
              <Box display="flex" justifyContent="center" pt={0.5}>
                <Button
                  size="small"
                  onClick={() => subscriptions.fetchNextPage()}
                  disabled={isFetchingNextPage}
                >
                  {isFetchingNextPage ? (
                    <SpinnerProgress size={16} />
                  ) : (
                    t('global_load_more')
                  )}
                </Button>
              </Box>
            )}
          </Box>
        )
      )}
    </Box>
  );

  return (
    <Tooltip
      title={tooltipTitle}
      placement="bottom-start"
      onOpen={onOpen}
      componentsProps={{
        tooltip: {
          sx: { maxWidth: 420, p: 1.5 },
        },
      }}
    >
      <Box display="inline-flex" alignItems="center" gap={0.5}>
        <PlanSubscriptionCount count={plan.subscriptionCount} />
      </Box>
    </Tooltip>
  );
};
