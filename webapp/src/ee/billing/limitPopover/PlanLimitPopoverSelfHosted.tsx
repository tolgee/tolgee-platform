import React, { FC } from 'react';
import { Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { getSelfHostedProgressData } from '../getSelfHostedProgressData';

type SelfHostedPlanLimitPopoverProps = PlanLimitPopoverWrapperProps;

export const PlanLimitPopoverSelfHosted: FC<
  React.PropsWithChildren<SelfHostedPlanLimitPopoverProps>
> = ({ open, onClose }) => {
  const usageLoadable = useApiQuery({
    url: '/v2/ee-current-subscription-usage',
    method: 'get',
    options: {
      enabled: open,
      refetchOnMount: true,
    },
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
  });

  const progressData =
    usageLoadable.data &&
    getSelfHostedProgressData({ usage: usageLoadable.data });

  return (
    <GenericPlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={usageLoadable.data?.isPayAsYouGo}
      progressData={progressData}
      loading={usageLoadable.isLoading}
      additionalContent={
        progressData?.wordsProgress?.isInUse && (
          <Typography variant="caption" color="text.secondary">
            <T
              keyName="self_hosted_words_usage_reported_periodically"
              defaultValue="The word count is reported periodically, so this figure can be a few minutes behind. Limits are checked against the live count when you write, not against this figure."
            />
          </Typography>
        )
      }
    />
  );
};
