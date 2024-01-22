import { styled, Tooltip, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

const StyledHint = styled(Box)`
  display: inline;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 0.2em;
  text-decoration-thickness: 1%;
`;

export const StringsHint: React.FC = ({ children }) => {
  const { t } = useTranslate();
  return (
    <Tooltip disableInteractive title={t('global_strings_hint')}>
      <StyledHint>{children}</StyledHint>
    </Tooltip>
  );
};
