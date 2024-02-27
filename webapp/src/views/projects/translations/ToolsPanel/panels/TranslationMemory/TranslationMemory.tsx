import { useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { green, grey, orange } from '@mui/material/colors';
import { TabMessage } from '../../common/TabMessage';
import { PanelContentProps } from '../../common/types';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { stringHash } from 'tg.fixtures/stringHash';
import { useEffect } from 'react';
import { TranslationWithPlaceholders } from 'tg.views/projects/translations/translationVisual/TranslationWithPlaceholders';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledItem = styled('div')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(0.5, 0.75)};
  margin: ${({ theme }) => theme.spacing(0.5, 0.5)};
  border-radius: 4px;
  gap: 0px 10px;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto 3px auto;
  grid-template-areas:
    'target target'
    'base base'
    'space space'
    'similarity source';
  font-size: 14px;
  cursor: pointer;
  color: ${({ theme }) => theme.palette.text.primary};
  transition: all 0.1s ease-in-out;
  transition-property: background color;
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[100]};
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledTarget = styled('div')`
  grid-area: target;
  font-size: 15px;
`;

const StyledBase = styled('div')`
  grid-area: base;
  font-style: italic;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledSimilarity = styled('div')`
  grid-area: similarity;
  font-size: 13px;
  color: white;
  padding: 1px 9px;
  border-radius: 10px;
`;

const StyledSource = styled('div')`
  grid-area: source;
  font-size: 13px;
  align-self: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const TranslationMemory: React.FC<PanelContentProps> = ({
  keyData,
  language,
  baseLanguage,
  project,
  setValue,
  setItemsCount,
}) => {
  const { t } = useTranslate();

  const deps = {
    keyId: keyData.keyId,
    targetLanguageId: language.id,
  };

  const dependenciesHash = stringHash(JSON.stringify(deps));

  const memory = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/translation-memory',
    method: 'post',
    // @ts-ignore add all dependencies to properly update query
    query: { hash: dependenciesHash, size: 2 },
    path: { projectId: project.id },
    content: {
      'application/json': deps,
    },
  });

  const data = memory.data;
  const items = data?._embedded?.translationMemoryItems;

  useEffect(() => {
    setItemsCount(items?.length ?? 0);
  }, [items?.length]);

  if (!data) {
    return null;
  }

  return (
    <StyledContainer>
      {items?.length ? (
        items.map((item, i) => {
          const similarityColor =
            item.similarity === 1
              ? green[600]
              : item.similarity > 0.7
              ? orange[800]
              : grey[600];
          return (
            <StyledItem
              key={i}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={() => {
                setValue(item.targetText);
              }}
              role="button"
              data-cy="translation-tools-translation-memory-item"
            >
              <StyledTarget>
                <TranslationWithPlaceholders
                  content={item.targetText}
                  locale={language.tag}
                  nested={false}
                />
              </StyledTarget>
              <StyledBase>
                <TranslationWithPlaceholders
                  content={item.baseText}
                  locale={baseLanguage.tag}
                  nested={false}
                />
              </StyledBase>
              <StyledSimilarity style={{ background: similarityColor }}>
                {Math.round(100 * item.similarity)}%
              </StyledSimilarity>
              <StyledSource>{item.keyName}</StyledSource>
            </StyledItem>
          );
        })
      ) : (
        <TabMessage>
          {t('translation_tools_nothing_found', 'Nothing found')}
        </TabMessage>
      )}
    </StyledContainer>
  );
};
