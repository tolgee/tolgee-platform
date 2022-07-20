import React from 'react';
import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { ToolsTab } from './ToolsTab';
import { useTranslationTools } from './useTranslationTools';
import { TranslationMemory } from './TranslationMemory';
import { MachineTranslation } from './MachineTranslation';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useConfig } from 'tg.globalContext/helpers';

const HORIZONTAL_BRAKEPOINT = 500;

const StyledContainer = styled('div')`
  overflow: auto;
`;

const StyledGrid = styled('div')`
  display: grid;
  grid-auto-flow: dense;
`;

const StyledLoadingWrapper = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  border-radius: 4px 4px 0px 0px;
  overflow: hidden;
`;

const StyledSmoothProgress = styled(SmoothProgress)`
  background: ${({ theme }) => theme.palette.emphasis[400]};
`;

export type Props = {
  width: number | string;
  data: ReturnType<typeof useTranslationTools>;
};

const TranslationTools = React.memo(function TranslationTools({
  width,
  data,
}: Props) {
  const t = useTranslate();
  const config = useConfig();

  const isVertical = width === undefined || width < HORIZONTAL_BRAKEPOINT;

  const mtEnabled = Object.values(
    config?.machineTranslationServices.services
  ).some(({ enabled }) => enabled);

  const numberOfItems = mtEnabled ? 2 : 1;

  const gridTemplateColumns = isVertical ? '1fr' : '1fr '.repeat(numberOfItems);

  return (
    <StyledContainer style={{ width }}>
      <StyledGrid style={{ gridTemplateColumns }}>
        <ToolsTab
          title={t(
            'translation_tools_translation_memory',
            'Translation memory'
          )}
          icon={<TranslationMemoryIcon fontSize="small" color="inherit" />}
          badgeNumber={
            data.memory?.data?._embedded?.translationMemoryItems?.length
          }
          data={data.memory}
        >
          <TranslationMemory
            data={data.memory?.data}
            operationsRef={data.operationsRef}
          />
        </ToolsTab>

        {mtEnabled && (
          <ToolsTab
            title={t(
              'translation_tools_machine_translation',
              'Machine translation'
            )}
            icon={<MachineTranslationIcon fontSize="small" color="inherit" />}
            badgeNumber={
              Object.keys(data.machine?.data?.machineTranslations || {}).length
            }
            data={data.machine}
          >
            <MachineTranslation
              data={data.machine?.data}
              operationsRef={data.operationsRef}
            />
          </ToolsTab>
        )}
      </StyledGrid>
      <StyledLoadingWrapper>
        <StyledSmoothProgress loading={data.isFetching} />
      </StyledLoadingWrapper>
    </StyledContainer>
  );
});

export default TranslationTools;
