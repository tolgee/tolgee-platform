import React from 'react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { useGlossaryTermHighlights } from '../hooks/useGlossaryTermHighlights';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { T } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { GlossaryTermPreview } from './GlossaryTermPreview';
import { LinkExternal } from 'tg.component/LinkExternal';
import { useProjectGlossaries } from 'tg.ee.module/glossary/hooks/useProjectGlossaries';
import { GlossaryLinksList } from 'tg.ee.module/glossary/components/GlossaryLinksList';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

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
  const { preferredOrganization } = usePreferredOrganization();

  const terms = useGlossaryTermsHighlightsForPanel(data);
  const editEnabled = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );
  const assignedGlossaries = useProjectGlossaries({
    projectId: project.id,
    enabled: terms.data.length === 0,
  });

  if (terms.data.length === 0) {
    const organizationSlug = project.organizationOwner?.slug;
    const hasAssignedGlossaries =
      assignedGlossaries.data && assignedGlossaries.data.length > 0;

    return (
      <StyledContainer data-cy="glossary-panel-container-empty">
        <TabMessage>
          {hasAssignedGlossaries && organizationSlug ? (
            <>
              <T keyName="translation_tools_glossary_no_terms_in_prefix" />{' '}
              <GlossaryLinksList
                glossaries={assignedGlossaries.data ?? []}
                organizationSlug={organizationSlug}
              />
            </>
          ) : (
            <T
              keyName="translation_tools_glossary_no_terms_no_glossary"
              params={{
                glossariesLink: (
                  <LinkExternal
                    href={LINKS.ORGANIZATION_GLOSSARIES.build({
                      [PARAMS.ORGANIZATION_SLUG]:
                        project.organizationOwner?.slug || '',
                    })}
                  />
                ),
              }}
            />
          )}
        </TabMessage>
      </StyledContainer>
    );
  }

  const found: number[] = [];
  const previews = terms.data
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
          editEnabled={editEnabled}
          onTranslationUpdated={() => terms.refetch()}
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
  useGlossaryTermsHighlightsForPanel(data).data.length;
