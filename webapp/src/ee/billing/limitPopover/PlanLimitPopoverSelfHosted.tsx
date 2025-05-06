import React, { FC } from 'react';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { getSelfHostedProgressData } from '../getSelfHostedProgressData';

type SelfHostedPlanLimitPopoverProps = PlanLimitPopoverWrapperProps;

export const PlanLimitPopoverSelfHosted: FC<
  SelfHostedPlanLimitPopoverProps
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
    />
  );
};
