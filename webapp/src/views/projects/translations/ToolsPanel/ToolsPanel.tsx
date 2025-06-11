import { useMemo } from 'react';
import { Box, IconButton, styled, Typography } from '@mui/material';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { Panel } from './common/Panel';

import { getPanels, PANELS_WHEN_INACTIVE } from './panelsList';
import { useOpenPanels } from './useOpenPanels';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { usePanelData } from './usePanelData';

const StyledButton = styled(IconButton)`
  position: absolute;
  top: 0px;
  right: 0px;
`;

const StyledTitle = styled(Box)`
  margin-top: 6px;
  margin-left: 8px;
  padding-left: 8px;
`;

const StyledWrapper = styled('div')`
  display: grid;
  padding: 8px 0px 8px 0px;
  padding-bottom: 100px;
`;

const StyledPanelList = styled(Box)`
  display: grid;
  position: relative;
`;

export const ToolsPanel = () => {
  const keyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const languageTag = useTranslationsSelector((c) => c.cursor?.language);
  const translations = useTranslationsSelector((c) => c.translations);
  const languages = useTranslationsSelector((c) => c.languages);
  const { setSidePanelOpen } = useTranslationsActions();

  const [openPanels, setOpenPanels] = useOpenPanels();

  const keyData = useMemo(() => {
    return translations?.find((t) => t.keyId === keyId);
  }, [keyId, translations]);

  const language = useMemo(() => {
    return languages?.find((l) => l.tag === languageTag);
  }, [languageTag, languages]);

  const baseLanguage = useMemo(() => {
    return languages?.find((l) => l.base);
  }, [languages]);
  const displayPanels = keyData && language && baseLanguage;
  const dataProps = usePanelData();

  return (
    <StyledWrapper>
      {displayPanels ? (
        <StyledPanelList>
          {getPanels()
            .filter(
              ({ displayPanel }) => !displayPanel || displayPanel(dataProps)
            )
            .map((config) => (
              <Panel
                {...config}
                key={config.id}
                data={dataProps}
                onToggle={() => {
                  if (openPanels.includes(config.id)) {
                    setOpenPanels(openPanels.filter((i) => i !== config.id));
                  } else {
                    setOpenPanels([...openPanels, config.id]);
                  }
                }}
                open={openPanels.includes(config.id)}
              />
            ))}
        </StyledPanelList>
      ) : (
        <StyledPanelList>
          <StyledTitle>
            <Typography variant="subtitle1" mb={2}>
              <T keyName="tools_panel_hint" />
            </Typography>
          </StyledTitle>
          <StyledButton onClick={() => setSidePanelOpen(false)}>
            <XClose />
          </StyledButton>
          {PANELS_WHEN_INACTIVE.map((config) => (
            <Panel
              {...config}
              key={config.id}
              data={dataProps}
              onToggle={() => {
                if (openPanels.includes(config.id)) {
                  setOpenPanels(openPanels.filter((i) => i !== config.id));
                } else {
                  setOpenPanels([...openPanels, config.id]);
                }
              }}
              open={openPanels.includes(config.id)}
            />
          ))}
        </StyledPanelList>
      )}
    </StyledWrapper>
  );
};
