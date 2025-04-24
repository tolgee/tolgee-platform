import React from 'react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useGlossaryTermHighlights } from '../hooks/useGlossaryTermHighlights';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { T } from '@tolgee/react';
import { Box, Button, styled } from '@mui/material';
import { LinkExternal02 } from '@untitled-ui/icons-react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { GlossaryTermPreview } from './GlossaryTermPreview';

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
  gap: ${({ theme }) => theme.spacing(2)};
  margin: ${({ theme }) => theme.spacing(0, 1.5)};
`;

// const StyledGap = styled('div')`
//   height: ${({ theme }) => theme.spacing(0.25)};
// `;

const fetchTermsHighlights = ({ keyData, baseLanguage }: PanelContentData) => {
  const languageTag = baseLanguage.tag;
  const text = keyData.translations[languageTag]?.text;

  return useGlossaryTermHighlights({ text, languageTag });
};

export const GlossariesPanel: React.VFC<PanelContentProps> = (data) => {
  const { language, baseLanguage, project } = data;
  const terms = fetchTermsHighlights(data);

  if (terms.length === 0) {
    return (
      <StyledContainer>
        <TabMessage>
          <T keyName="translation_tools_glossary_no_terms" />
        </TabMessage>
        <StyledContent>
          <Box>
            <Button
              color="primary"
              variant="outlined"
              startIcon={<LinkExternal02 />}
              component={Link}
              to={LINKS.ORGANIZATION_GLOSSARIES.build({
                [PARAMS.ORGANIZATION_SLUG]: project.organizationOwner!.slug,
              })}
            >
              <T keyName="translation_tools_glossary_open_glossaries" />
            </Button>
          </Box>
        </StyledContent>
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
        />
      );
    });
  return (
    <StyledContainer>
      {/*<StyledGap />*/}
      <StyledContent>{previews}</StyledContent>
    </StyledContainer>
  );
};

export const glossariesCount = (data: PanelContentData) =>
  fetchTermsHighlights(data).length;
