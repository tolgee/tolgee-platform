import { Help } from '@mui/icons-material';
import { styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

const StyledPrimaryService = styled('div')`
  display: flex;
  gap: 4px;
  align-items: center;
`;

const StyledHelpIcon = styled(Help)`
  font-size: 15px;
`;

export const PrimaryServiceLabel = () => {
  const { t } = useTranslate();
  return (
    <Tooltip title={t('project_languages_primary_provider_hint')}>
      <StyledPrimaryService>
        <div>{t('project_languages_primary_provider', 'Primary')}</div>
        <StyledHelpIcon />
      </StyledPrimaryService>
    </Tooltip>
  );
};
