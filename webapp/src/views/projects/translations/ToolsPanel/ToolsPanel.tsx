import { useEffect, useMemo, useRef } from 'react';
import { Box, IconButton, styled, Typography } from '@mui/material';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { Panel } from './common/Panel';

import { getPanels, PANELS_WHEN_INACTIVE } from './panelsList';
import { useOpenPanels } from './useOpenPanels';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { usePanelData } from './usePanelData';
import { useAppToolsPanels } from './panels/AppPanel/useAppToolsPanels';
import {
  consumePendingPanelReveal,
  onPanelRevealRequest,
} from '../decorators/panelRevealEvent';

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
  const project = useProject();
  const keyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const languageTag = useTranslationsSelector((c) => c.cursor?.language);
  const translations = useTranslationsSelector((c) => c.translations);
  const languages = useTranslationsSelector((c) => c.languages);
  const { setSidePanelOpen } = useTranslationsActions();

  const { openPanels, togglePanelOpen, ensurePanelOpen } = useOpenPanels();
  const appPanels = useAppToolsPanels(project.id);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  const reveal = (id: string) => {
    setSidePanelOpen(true);
    ensurePanelOpen(id);
    // Defer scrolling until after the open state propagates and the
    // panel content is mounted (requires the cursor to be set, which
    // the caller should have done before requesting the reveal).
    requestAnimationFrame(() => {
      const el = wrapperRef.current?.querySelector(
        `[data-cy-id="${CSS.escape(id)}"][data-cy="translation-panel"]`
      );
      (el as HTMLElement | null)?.scrollIntoView({
        block: 'nearest',
        behavior: 'smooth',
      });
    });
  };

  useEffect(() => {
    // Two paths: (a) reveal arrived while listener was mounted → live
    // event handler. (b) reveal arrived before ToolsPanel mounted
    // (side panel was closed at click time) → stored as pending and
    // consumed here on mount.
    const pending = consumePendingPanelReveal();
    if (pending) reveal(pending);
    return onPanelRevealRequest(reveal);
  }, []);

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

  const allPanels = useMemo(() => [...getPanels(), ...appPanels], [appPanels]);

  return (
    <StyledWrapper ref={wrapperRef}>
      {displayPanels ? (
        <StyledPanelList>
          {allPanels
            .filter(
              ({ displayPanel }) => !displayPanel || displayPanel(dataProps)
            )
            .map((config) => (
              <Panel
                {...config}
                key={config.id}
                data={dataProps}
                onToggle={() => {
                  togglePanelOpen(config.id);
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
                togglePanelOpen(config.id);
              }}
              open={openPanels.includes(config.id)}
            />
          ))}
        </StyledPanelList>
      )}
    </StyledWrapper>
  );
};
