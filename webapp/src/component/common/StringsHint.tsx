import { useTranslate } from '@tolgee/react';
import { Tooltip } from '@mui/material';
import { StyledBillingHint } from '../billing/Decorations';

export const StringsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_strings_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};

export const KeysHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_keys_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};

export const SeatsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_seats_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};
