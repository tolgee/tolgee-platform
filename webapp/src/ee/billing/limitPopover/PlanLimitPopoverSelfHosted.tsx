import React, { FC } from 'react';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';
import { ProgressItem } from '../component/getProgressData';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

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

  const infoLoadable = useApiQuery({
    url: '/v2/ee-license/info',
    method: 'get',
    options: {
      enabled: open,
    },
  });

  const progressData =
    usageLoadable.data && getProgressData({ usage: usageLoadable.data });

  return (
    <GenericPlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={infoLoadable.data?.isPayAsYouGo}
      progressData={progressData}
      loading={usageLoadable.isLoading || infoLoadable.isLoading}
    />
  );
};

const getProgressData = ({
  usage,
}: {
  usage: components['schemas']['CurrentUsageModel'];
}) => {
  const keysProgress = new ProgressItem(
    usage.keys.included,
    usage.keys.current
  );

  const seatsProgress = new ProgressItem(
    usage.seats.included,
    usage.seats.current
  );

  const creditsProgress = new ProgressItem(
    usage.credits.included,
    usage.credits.current
  );

  return {
    keysProgress,
    seatsProgress,
    creditProgress: creditsProgress,
  };
};
