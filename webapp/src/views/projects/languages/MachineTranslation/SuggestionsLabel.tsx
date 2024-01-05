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

export const SuggestionsLabel = () => {
  const { t } = useTranslate();
  return (
    <Tooltip title={t('project_mt_dialog_service_suggested_hint')}>
      <StyledPrimaryService>
        <div>{t('project_mt_dialog_service_suggested')}</div>
        <StyledHelpIcon />
      </StyledPrimaryService>
    </Tooltip>
  );
};
