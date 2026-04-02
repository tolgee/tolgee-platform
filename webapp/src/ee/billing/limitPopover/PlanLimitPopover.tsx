import { useConfig } from 'tg.globalContext/helpers';
import { PlanLimitPopoverCloud } from './PlanLimitPopoverCloud';
import { PlanLimitPopoverSelfHosted } from './PlanLimitPopoverSelfHosted';
import { PlanLimitPopoverWrapperProps } from './generic/PlanLimitPopoverWrapper';

type Props = PlanLimitPopoverWrapperProps;

export const PlanLimitPopover: React.FC<Props> = ({ open, onClose }) => {
  const config = useConfig();

  if (config.billing.enabled) {
    return <PlanLimitPopoverCloud onClose={onClose} open={open} />;
  }

  return <PlanLimitPopoverSelfHosted onClose={onClose} open={open} />;
};
