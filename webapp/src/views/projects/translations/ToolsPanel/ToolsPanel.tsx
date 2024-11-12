import { useMemo } from 'react';
import { Box, IconButton, Typography, styled } from '@mui/material';
import { useProject } from 'tg.hooks/useProject';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { Panel } from './common/Panel';

import { PANELS, PANELS_WHEN_INACTIVE } from './panelsList';
import { useOpenPanels } from './useOpenPanels';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const StyledButton = styled(IconButton)`
  position: absolute;
  top: 0px;
  right: 0px;
`;

const StyledTitle = styled(Box)`
  margin-top: 6px;
  margin-left: 8px;
`;

const StyledWrapper = styled('div')`
  display: grid;
  padding: 8px 0px 8px 8px;
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
  const activeVariant = useTranslationsSelector((c) => c.cursor?.activeVariant);
  const translations = useTranslationsSelector((c) => c.translations);
  const languages = useTranslationsSelector((c) => c.languages);
  const { setEditValueString, setSidePanelOpen } = useTranslationsActions();

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
  const translation = language?.tag
    ? keyData?.translations[language.tag]
    : undefined;

  const displayPanels = keyData && language && baseLanguage;
  const projectPermissions = useProjectPermissions();

  const dataProps = {
    project,
    keyData: keyData!,
    language: language!,
    baseLanguage: baseLanguage!,
    activeVariant: keyData?.keyIsPlural ? activeVariant! : undefined,
    setValue: setEditValueString,
    editEnabled: language
      ? (projectPermissions.satisfiesLanguageAccess(
          'translations.edit',
          language.id
        ) &&
          translation?.state !== 'DISABLED') ||
        Boolean(
          keyData?.tasks?.find(
            (t) =>
              t.languageTag === language.tag &&
              t.userAssigned &&
              t.type === 'TRANSLATE'
          )
        )
      : false,
    projectPermissions,
  };

  return (
    <StyledWrapper>
      {displayPanels ? (
        <StyledPanelList>
          {PANELS.filter(
            ({ displayPanel }) => !displayPanel || displayPanel(dataProps)
          ).map((config) => (
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
