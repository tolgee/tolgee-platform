import { useEffect, useMemo, useRef } from 'react';
import { Box, IconButton, styled } from '@mui/material';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { Panel } from './common/Panel';

import { getPanels, PANELS_WHEN_INACTIVE } from './panelsList';
import { useOpenPanels } from './useOpenPanels';
import { XClose } from '@untitled-ui/icons-react';
import { usePanelData } from './usePanelData';
import { useAppToolsPanels } from './panels/AppPanel/useAppToolsPanels';
import { useAppEmptyPanels } from './panels/AppPanel/useAppEmptyPanels';
import {
  consumePendingPanelReveal,
  onPanelRevealRequest,
} from '../decorators/panelRevealEvent';

const StyledButton = styled(IconButton)`
  position: absolute;
  top: 0px;
  right: 0px;
  /* Sit above the panels' sticky headers (z-index 3) so it stays clickable in
   * the header's empty right area. */
  z-index: 4;
`;

const StyledWrapper = styled('div')`
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  padding: 8px 0px 8px 0px;
  padding-bottom: 100px;
`;

const StyledPanelList = styled(Box)`
  display: grid;
  grid-template-columns: minmax(0, 1fr);
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
  const appEmptyPanels = useAppEmptyPanels(project.id);
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

  useEffect(() => {
    // Expand app plugin panels the first time they appear, but don't re-open
    // one the user has since collapsed — track which ids we've already
    // auto-opened so the default applies once per panel.
    const SEEN_KEY = '__tolgee_seenAppPanels';
    let seen: string[] = [];
    try {
      const raw = localStorage.getItem(SEEN_KEY);
      if (Array.isArray(JSON.parse(raw ?? ''))) seen = JSON.parse(raw ?? '');
    } catch (e) {
      // ignore malformed storage
    }
    const fresh = appEmptyPanels
      .map((p) => p.id)
      .filter((id) => !seen.includes(id));
    if (fresh.length === 0) return;
    fresh.forEach((id) => ensurePanelOpen(id));
    localStorage.setItem(SEEN_KEY, JSON.stringify([...seen, ...fresh]));
  }, [appEmptyPanels]);

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
          <StyledButton onClick={() => setSidePanelOpen(false)}>
            <XClose />
          </StyledButton>
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
          <StyledButton onClick={() => setSidePanelOpen(false)}>
            <XClose />
          </StyledButton>
          {[...appEmptyPanels, ...PANELS_WHEN_INACTIVE].map((config) => (
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
