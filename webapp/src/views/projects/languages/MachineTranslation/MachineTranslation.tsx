import { Box, Typography, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { useMachineTranslationSettings } from './useMachineTranslationSettings';
import { StyledLanguageTable } from '../../../../component/languages/tableStyles';
import { SettingsTable } from './SettingsTable';
import { SmoothProgress } from 'tg.component/SmoothProgress';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledLoadingWrapper = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
`;

export const MachineTranslation = () => {
  const { t } = useTranslate();
  const { settings, applyUpdate, isFetching } = useMachineTranslationSettings();
  const gridTemplateColumns = `1fr auto 1fr auto`;

  if (!settings) {
    return null;
  }

  return (
    <Box>
      <Box mt={4} mb={3}>
        <Typography variant="h5">
          <T keyName="machine_translation_title" />
        </Typography>
      </Box>
      <QuickStartHighlight
        itemKey="machine_translation"
        message={t('quick_start_item_machine_translation_hint')}
        borderRadius="5px"
        offset={10}
      >
        <StyledContainer>
          <StyledLanguageTable style={{ gridTemplateColumns }}>
            <SettingsTable settings={settings || []} onUpdate={applyUpdate} />
            <StyledLoadingWrapper>
              <SmoothProgress loading={isFetching} />
            </StyledLoadingWrapper>
          </StyledLanguageTable>
        </StyledContainer>
      </QuickStartHighlight>
    </Box>
  );
};
