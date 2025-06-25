import { styled } from '@mui/material';
import { PanelHeader } from '../ToolsPanel/common/PanelHeader';
import { useTranslate } from '@tolgee/react';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

const OPEN_SUGGESTIONS_KEY = '__tolgee_suggestions_hidden';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

type TranslationSuggestionModel =
  components['schemas']['TranslationSuggestionModel'];

const StyledContainer = styled('div')`
  display: grid;
  padding: 6px 8px;
  gap: 8px;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
`;

const StyledScrollWrapper = styled('div')`
  display: grid;
  max-height: 400px;
  overflow: auto;
`;

const StyledItemsWrapper = styled('div')`
  display: grid;
  gap: 8px;
`;

type Props = {
  countContent: React.ReactNode;
  suggestions: TranslationSuggestionSimpleModel[];
  keyId: number;
  languageId: number;
  isPlural: boolean;
  locale: string;
};

export const SuggestionsList = ({
  countContent,
  suggestions,
  keyId,
  languageId,
  isPlural,
  locale,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const suggestionsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/translation-suggestion',
    method: 'get',
    query: {
      filterKeyId: [keyId],
      filterLanguageId: [languageId],
    },
    path: {
      projectId: project.id,
    },
  });

  const [hidden, setHidden] = useLocalStorageState({
    key: OPEN_SUGGESTIONS_KEY,
    initial: undefined,
  });
  const panelId = 'suggestions';

  return (
    <StyledContainer>
      <StyledHeader>
        <PanelHeader
          sx={{ height: 'unset', padding: 0, background: 'unset' }}
          icon={null}
          name={t('translation_tools_suggestions')}
          countContent={countContent}
          onToggle={() => {
            setHidden((value) => (value ? undefined : 'true'));
          }}
          panelId={panelId}
          open={!hidden}
        />
      </StyledHeader>
      {!hidden && (
        <StyledScrollWrapper>
          <StyledItemsWrapper>
            {(
              suggestionsLoadable.data?._embedded?.suggestions || suggestions
            ).map((item) => {
              const itemWithDate = item as Partial<TranslationSuggestionModel>;
              return (
                <TranslationSuggestion
                  key={item.id}
                  suggestion={item}
                  isPlural={isPlural}
                  locale={locale}
                  lastUpdated={itemWithDate.updatedAt ?? itemWithDate.createdAt}
                />
              );
            })}
          </StyledItemsWrapper>
        </StyledScrollWrapper>
      )}
    </StyledContainer>
  );
};
