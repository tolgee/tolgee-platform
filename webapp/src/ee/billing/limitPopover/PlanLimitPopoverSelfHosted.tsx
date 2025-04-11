import React, { FC } from 'react';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';
import { ProgressItem } from '../component/getProgressData';
import { PlanLimitPopover } from './generic/PlanLimitPopover';
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
    <PlanLimitPopover
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
  usage: components['schemas']['Curre'];
}) => {
  const keysProgress = new ProgressItem(
    usage.keys.usedQuantity - usage.keys.usedQuantityOverPlan,
    usage.keys.usedQuantity
  );

  const seatsProgress = new ProgressItem(
    usage.seats.usedQuantity - usage.seats.usedQuantityOverPlan,
    usage.seats.usedQuantity
  );

  function getCreditProgress() {
    if (!usage.credits) {
      return undefined;
    }
    return new ProgressItem(
      usage.credits.usedQuantity - usage.credits.usedQuantityOverPlan,
      usage.credits.usedQuantity
    );
  }

  return {
    keysProgress,
    seatsProgress,
    creditProgress: getCreditProgress(),
  };
};
