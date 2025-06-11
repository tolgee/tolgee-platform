import React from 'react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useGlossaryTermHighlights } from '../hooks/useGlossaryTermHighlights';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { T } from '@tolgee/react';
import { Box, Link, styled } from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { GlossaryTermPreview } from './GlossaryTermPreview';
import { Link as RouterLink } from 'react-router-dom';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 4px;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledContent = styled(Box)`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin: ${({ theme }) => theme.spacing(0, 0.5)};
`;

const useGlossaryTermsHighlightsForPanel = ({
  keyData,
  baseLanguage,
}: PanelContentData) => {
  const languageTag = baseLanguage.tag;
  const text = keyData.translations[languageTag]?.text;

  return useGlossaryTermHighlights({ text, languageTag });
};

export const GlossariesPanel: React.VFC<PanelContentProps> = (data) => {
  const { language, baseLanguage, project, appendValue } = data;
  const terms = useGlossaryTermsHighlightsForPanel(data);

  if (terms.length === 0) {
    return (
      <StyledContainer data-cy="glossary-panel-container-empty">
        <TabMessage>
          <T
            keyName="translation_tools_glossary_no_terms"
            params={{
              glossariesLink: (
                <Link
                  component={RouterLink}
                  to={LINKS.ORGANIZATION_GLOSSARIES.build({
                    [PARAMS.ORGANIZATION_SLUG]: project.organizationOwner!.slug,
                  })}
                />
              ),
            }}
          />
        </TabMessage>
      </StyledContainer>
    );
  }

  const found: number[] = [];
  const previews = terms
    .map((v) => v.value)
    .filter((term) => {
      if (found.includes(term.id)) {
        return false;
      }
      found.push(term.id);
      return true;
    })
    .map((term) => {
      return (
        <GlossaryTermPreview
          key={term.id}
          term={term}
          languageTag={baseLanguage.tag}
          targetLanguageTag={language.tag}
          appendValue={appendValue}
          slim
        />
      );
    });
  return (
    <StyledContainer data-cy="glossary-panel-container">
      <StyledContent>{previews}</StyledContent>
    </StyledContainer>
  );
};

export const useGlossariesCount = (data: PanelContentData) =>
  useGlossaryTermsHighlightsForPanel(data).length;
