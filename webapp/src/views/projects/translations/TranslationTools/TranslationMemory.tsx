import { useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { green, grey, orange } from '@mui/material/colors';
import { TabMessage } from './TabMessage';
import { useTranslationTools } from './useTranslationTools';

type PagedModelTranslationMemoryItemModel =
  components['schemas']['PagedModelTranslationMemoryItemModel'];

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

type Props = {
  data: PagedModelTranslationMemoryItemModel | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
};

export const TranslationMemory: React.FC<Props> = ({ data, operationsRef }) => {
  const { t } = useTranslate();
  const items = data?._embedded?.translationMemoryItems;

  if (!data) {
    return null;
  }

  return (
    <StyledContainer>
      {items?.length ? (
        items.map((item) => {
          const similarityColor =
            item.similarity === 1
              ? green[600]
              : item.similarity > 0.7
              ? orange[800]
              : grey[600];
          return (
            <StyledItem
              key={item.keyName}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={() => {
                operationsRef.current.updateTranslation(item.targetText);
              }}
              role="button"
              data-cy="translation-tools-translation-memory-item"
            >
              <StyledTarget>{item.targetText}</StyledTarget>
              <StyledBase>{item.baseText}</StyledBase>
              <StyledSimilarity style={{ background: similarityColor }}>
                {Math.round(100 * item.similarity)}%
              </StyledSimilarity>
              <StyledSource>{item.keyName}</StyledSource>
            </StyledItem>
          );
        })
      ) : (
        <TabMessage
          type="placeholder"
          message={t('translation_tools_nothing_found', 'Nothing found')}
        />
      )}
    </StyledContainer>
  );
};
