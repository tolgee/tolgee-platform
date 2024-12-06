import { Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { StyledBillingHint } from 'tg.component/billing/Decorations';

export const MtHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_mt_credits_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};

export const StringSlotsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_string_slot_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};
