import { Box, Chip, Grid, styled, Typography } from '@mui/material';
import React from 'react';
import { useHistory } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { CircledLanguageIconList } from 'tg.component/languages/CircledLanguageIconList';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { TranslationMemoryListItemMenu } from 'tg.ee.module/translationMemory/components/TranslationMemoryListItemMenu';

type TranslationMemoryWithStatsModel =
  components['schemas']['TranslationMemoryWithStatsModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr auto 70px;
  grid-template-areas: 'name projects languages controls';
  padding: ${({ theme }) => theme.spacing(1.5, 2.5)};
  align-items: center;
  cursor: pointer;
  background-color: ${({ theme }) => theme.palette.background.default};
  @container (max-width: 599px) {
    grid-gap: ${({ theme }) => theme.spacing(1, 2)};
    grid-template-columns: 1fr 1fr 70px;
    grid-template-areas:
      'name      projects  controls'
      'languages languages languages';
  }
`;

const StyledName = styled('div')`
  grid-area: name;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  gap: 2px;
  @container (max-width: 599px) {
    margin-right: 0px;
  }
`;

const StyledNameRow = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
`;

const StyledProjects = styled('div')`
  grid-area: projects;
  display: flex;
  overflow: hidden;
  margin-right: ${({ theme }) => theme.spacing(2)};
  @container (max-width: 599px) {
    margin-right: 0px;
  }
`;

const StyledLanguages = styled('div')`
  grid-area: languages;
  @container (max-width: 599px) {
    justify-content: flex-start;
  }
`;

const StyledControls = styled('div')`
  grid-area: controls;
`;

const StyledNameText = styled(Typography)`
  font-size: 16px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
`;

type Props = {
  translationMemory: TranslationMemoryWithStatsModel;
};

export const TranslationMemoryListItem: React.VFC<Props> = ({
  translationMemory,
}) => {
  const { t } = useTranslate();
  const history = useHistory();
  const { preferredOrganization } = usePreferredOrganization();
  const languageTag = translationMemory.sourceLanguageTag;
  const languageData = languageInfo[languageTag];
  const languages = [
    {
      base: true,
      flagEmoji: languageData?.flags?.[0] || '',
      id: 0,
      name: languageData?.englishName || languageTag,
      originalName: languageData?.originalName || languageTag,
      tag: languageTag,
    },
  ];

  const projectNames = translationMemory.assignedProjectNames ?? [];
  const totalProjects = translationMemory.assignedProjectsCount;

  const isShared = translationMemory.type === 'SHARED';

  return (
    <StyledContainer
      role="button"
      data-cy="translation-memory-list-item"
      onClick={() =>
        history.push(
          LINKS.ORGANIZATION_TRANSLATION_MEMORY.build({
            [PARAMS.TRANSLATION_MEMORY_ID]: translationMemory.id,
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          })
        )
      }
    >
      <StyledName>
        <StyledNameRow>
          <StyledNameText variant="h3" data-cy="translation-memory-list-name">
            {translationMemory.name}
          </StyledNameText>
          <Chip
            size="small"
            label={
              isShared
                ? t('translation_memory_type_shared', 'Shared')
                : t('translation_memory_type_project_only', 'Project only')
            }
            color={isShared ? 'primary' : undefined}
            sx={(theme) => ({
              flexShrink: 0,
              ...(isShared
                ? {}
                : {
                    backgroundColor:
                      theme.palette.placeholders.variant.background,
                    color: theme.palette.placeholders.variant.text,
                    border: `1px solid ${theme.palette.placeholders.variant.border}`,
                  }),
            })}
          />
        </StyledNameRow>
        <Typography
          variant="caption"
          color="text.secondary"
          data-cy="translation-memory-list-entries-count"
        >
          <T
            keyName="translation_memory_list_entries_count"
            defaultValue="{count, plural, one {# entry} other {# entries}}"
            params={{ count: translationMemory.entryCount }}
          />
        </Typography>
      </StyledName>
      <StyledProjects>
        <Typography variant="body2" color="text.secondary" noWrap>
          {totalProjects === 0 && (
            <T
              keyName="translation_memory_list_no_project"
              defaultValue="No project"
            />
          )}
          {totalProjects > 0 && totalProjects <= 3 && projectNames.join(', ')}
          {totalProjects > 3 && (
            <T
              keyName="translation_memory_list_projects_count"
              defaultValue="{count, plural, one {# project} other {# projects}}"
              params={{ count: totalProjects }}
            />
          )}
        </Typography>
      </StyledProjects>
      <StyledLanguages data-cy="translation-memory-list-languages">
        <Grid container>
          <CircledLanguageIconList languages={languages} />
        </Grid>
      </StyledLanguages>
      <StyledControls>
        <Box width="100%" display="flex" justifyContent="flex-end">
          <TranslationMemoryListItemMenu
            translationMemory={translationMemory}
          />
        </Box>
      </StyledControls>
    </StyledContainer>
  );
};
