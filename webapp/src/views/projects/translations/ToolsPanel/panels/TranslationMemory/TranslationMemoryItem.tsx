import { styled } from '@mui/material';
import { green, grey, orange } from '@mui/material/colors';
import { getTolgeePlurals, getVariantExample } from '@tginternal/editor';
import { useMemo } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationWithPlaceholders } from 'tg.views/projects/translations/translationVisual/TranslationWithPlaceholders';
import {
  useBaseVariant,
  useExtractedPlural,
  useVariantExample,
} from '../../common/useExtractedPlural';

type TranslationMemoryItemModel =
  components['schemas']['TranslationMemoryItemModel'];

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
  item: TranslationMemoryItemModel;
  setValue(val: string): void;
  languageTag: string;
  baseLanguageTag: string;
  pluralVariant: string | undefined;
};

export const TranslationMemoryItem = ({
  item,
  languageTag,
  baseLanguageTag,
  setValue,
  pluralVariant,
}: Props) => {
  const similarityColor =
    item.similarity === 1
      ? green[600]
      : item.similarity > 0.7
      ? orange[800]
      : grey[600];

  const targetText = useExtractedPlural(pluralVariant, item.targetText);

  const variantExample = useVariantExample(pluralVariant, languageTag);

  const baseVariant = useBaseVariant(
    pluralVariant,
    languageTag,
    baseLanguageTag
  );

  const baseText = useExtractedPlural(baseVariant, item.baseText);

  const baseVariantExample = useVariantExample(baseVariant, baseLanguageTag);

  return (
    <StyledItem
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
          content={targetText}
          locale={languageTag}
          nested={Boolean(pluralVariant)}
          pluralExampleValue={variantExample}
        />
      </StyledTarget>
      <StyledBase>
        <TranslationWithPlaceholders
          content={baseText}
          locale={baseLanguageTag}
          nested={Boolean(baseVariant)}
          pluralExampleValue={baseVariantExample}
        />
      </StyledBase>
      <StyledSimilarity style={{ background: similarityColor }}>
        {Math.round(100 * item.similarity)}%
      </StyledSimilarity>
      <StyledSource>{item.keyName}</StyledSource>
    </StyledItem>
  );
};
