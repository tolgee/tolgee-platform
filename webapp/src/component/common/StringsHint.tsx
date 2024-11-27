import { useTranslate } from '@tolgee/react';
import { Tooltip } from '@mui/material';
import { StyledBillingHint } from 'tg.ee/billing/component/Decorations';

export const StringsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_strings_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};
