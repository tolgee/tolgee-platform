import { useEffect, useState } from 'react';
import { styled } from '@mui/material';
import { ExpandLess, ExpandMore } from '@mui/icons-material';

import { SmoothProgress } from 'tg.component/SmoothProgress';
import { StyledLanguageTable } from '../tableStyles';
import { useMachineTranslationSettings } from './useMachineTranslationSettings';
import { SettingsTable } from './SettingsTable';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledToggle = styled('div')`
  display: flex;
  justify-content: center;
  grid-column: 1 / -1;
  cursor: pointer;
  background-color: ${({ theme }) => theme.palette.emphasis[100]};
  transition: background-color 0.1s ease-in-out;
  &:active,
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[200]};
  }
`;

const StyledLoadingWrapper = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
`;

export const MachineTranslation = () => {
  const [expanded, setExpanded] = useState(false);

  const { settings, applyUpdate, isFetching } = useMachineTranslationSettings();

  const gridTemplateColumns = `1fr auto 1fr auto`;

  useEffect(() => {
    if (
      (settings || [])?.filter(
        ({ autoSettings, mtSettings }) => autoSettings || mtSettings
      ).length > 1
    ) {
      setExpanded(true);
    }
  }, [settings]);

  return (
    <>
      {settings && settings && (
        <StyledContainer>
          <StyledLanguageTable style={{ gridTemplateColumns }}>
            <SettingsTable
              settings={settings || []}
              expanded={expanded}
              onUpdate={applyUpdate}
            />

            {settings.length > 1 && (
              <StyledToggle
                role="button"
                data-cy="machine-translations-settings-toggle"
                onClick={() => setExpanded((expanded) => !expanded)}
              >
                {expanded ? <ExpandLess /> : <ExpandMore />}
              </StyledToggle>
            )}
            <StyledLoadingWrapper>
              <SmoothProgress loading={isFetching} />
            </StyledLoadingWrapper>
          </StyledLanguageTable>
        </StyledContainer>
      )}
    </>
  );
};
