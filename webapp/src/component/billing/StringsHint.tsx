import { Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { StyledBillingHint } from './Decorations';

export const StringsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_strings_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};
