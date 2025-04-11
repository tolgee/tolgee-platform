import React, { FC } from 'react';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';
import { getProgressData } from '../component/getProgressData';
import { PlanLimitPopover } from './generic/PlanLimitPopover';

type SelfHostedPlanLimitPopoverProps = PlanLimitPopoverWrapperProps;

export const PlanLimitPopoverSelfHosted: FC<
  SelfHostedPlanLimitPopoverProps
> = ({ open, onClose }) => {
  const progressData = usage && getProgressData({ usage });

  return progressData ? (
    <PlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={usage?.isPayAsYouGo}
      progressData={progressData}
    />
  ) : null;
};
