import { styled, Tooltip } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { TranslationWithPlaceholders } from 'tg.views/projects/translations/translationVisual/TranslationWithPlaceholders';
import {
  useBaseVariant,
  useExtractedPlural,
  useVariantExample,
} from '../../common/useExtractedPlural';
import { T } from '@tolgee/react';
import clsx from 'clsx';

type TranslationMemoryItemModel =
  components['schemas']['TranslationMemoryItemModel'];

// Layout mirrors the Claude-Design "Editor - TM panel" mock: a clickable row with the
// match score on the left, TM-name + key on the right, the target translation in
// emphasised text, then the source text muted underneath.
const StyledItem = styled('div')`
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: ${({ theme }) => theme.spacing(1.5, 2)};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  transition: background 0.1s ease-in-out;

  &:last-of-type {
    border-bottom: none;
  }
  &.clickable {
    cursor: pointer;
  }
  &.clickable:hover {
    background: ${({ theme }) => theme.palette.cell.hover};
  }
`;

const StyledHead = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
`;

// Score colour tiers come from the design: 100% green, 85–99% primary (pink), 60–84% amber,
// below 60% grey. Penalised matches add a yellow ring + a small dot inside the pill.
const StyledScore = styled('div')`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 9999px;
  font-family: 'Roboto Mono', monospace;
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.4px;
  cursor: help;
  white-space: nowrap;

  &.s-100 {
    color: #1f8a4d;
    background: #e6f8ef;
  }
  &.s-high {
    color: ${({ theme }) => theme.palette.primary.main};
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? 'rgba(238, 67, 99, 0.18)'
        : 'rgba(238, 67, 99, 0.10)'};
  }
  &.s-mid {
    color: #866a00;
    background: #fff6d6;
  }
  &.s-low {
    color: ${({ theme }) => theme.palette.text.secondary};
    background: ${({ theme }) => theme.palette.emphasis[100]};
  }
  &.penalized {
    border: 1px solid #ffce00;
    padding: 3px 7px;
  }
`;

const StyledScoreDot = styled('span')`
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c89800;
`;

const StyledMeta = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  letter-spacing: 0.4px;
  color: ${({ theme }) => theme.palette.text.secondary};
  min-width: 0;
  overflow: hidden;
`;

const StyledMetaItem = styled('span')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
`;

const StyledMetaSeparator = styled('span')`
  flex-shrink: 0;
  opacity: 0.6;
`;

const StyledTarget = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.primary};
  overflow-wrap: break-word;
  overflow: hidden;
`;

const StyledSource = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow-wrap: break-word;
  overflow: hidden;
`;

const StyledEmptyText = styled('span')`
  font-style: italic;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  item: TranslationMemoryItemModel;
  setValue(val: string): void;
  languageTag: string;
  baseLanguageTag: string;
  pluralVariant: string | undefined;
};

const tierClass = (similarityPercent: number): string => {
  if (similarityPercent >= 100) return 's-100';
  if (similarityPercent >= 85) return 's-high';
  if (similarityPercent >= 60) return 's-mid';
  return 's-low';
};

export const TranslationMemoryItem = ({
  item,
  languageTag,
  baseLanguageTag,
  setValue,
  pluralVariant,
}: Props) => {
  const formatTimeDistance = useTimeDistance();
  const targetText = useExtractedPlural(pluralVariant, item.targetText);
  const variantExample = useVariantExample(pluralVariant, languageTag);
  const baseVariant = useBaseVariant(
    pluralVariant,
    languageTag,
    baseLanguageTag
  );
  const baseText = useExtractedPlural(baseVariant, item.baseText);
  const baseVariantExample = useVariantExample(baseVariant, baseLanguageTag);

  const rawSimilarity = item.rawSimilarity ?? item.similarity;
  const displayedPercent = Math.round(100 * item.similarity);
  const rawPercent = Math.round(100 * rawSimilarity);
  const penalty = rawPercent - displayedPercent;

  // Backend returns updatedAt as an ISO string (sourced from the entry for stored rows or
  // from the contributing translation for virtual rows). useTimeDistance produces locale-aware
  // "X minutes / hours / days" strings shared with the rest of the app; the absolute timestamp
  // goes on the title for hover so the relative label stays compact.
  const updatedAtDate = item.updatedAt ? new Date(item.updatedAt) : null;
  const updatedAtValid =
    updatedAtDate !== null && !isNaN(updatedAtDate.getTime());
  const updatedAtLabel = updatedAtValid
    ? formatTimeDistance(updatedAtDate as Date)
    : null;
  const updatedAtAbsolute = updatedAtValid
    ? (updatedAtDate as Date).toLocaleString()
    : undefined;

  const scoreBadge = (
    <StyledScore
      className={clsx(tierClass(displayedPercent), {
        penalized: penalty > 0,
      })}
    >
      {displayedPercent}%{penalty > 0 && <StyledScoreDot />}
    </StyledScore>
  );

  return (
    <StyledItem
      onMouseDown={(e) => e.preventDefault()}
      onClick={() => {
        if (targetText) setValue(targetText);
      }}
      className={clsx({ clickable: Boolean(targetText) })}
      role="button"
      data-cy="translation-tools-translation-memory-item"
    >
      <StyledHead>
        {penalty > 0 ? (
          <Tooltip
            title={
              <T
                keyName="translation_memory_penalty_tooltip"
                defaultValue="{raw}% raw − {penalty} penalty"
                params={{ raw: rawPercent, penalty }}
              />
            }
          >
            {scoreBadge}
          </Tooltip>
        ) : (
          scoreBadge
        )}
        <StyledMeta>
          {(() => {
            // Compose the meta line from up to three parts: TM name, key reference,
            // relative-time label. Render dot separators only between parts that are
            // actually present so we don't get leading/trailing/double dots.
            const parts: React.ReactNode[] = [];
            if (item.translationMemoryName) {
              parts.push(
                <StyledMetaItem
                  key="tm"
                  title={item.translationMemoryName}
                  data-cy="translation-tools-translation-memory-item-tm-name"
                >
                  {item.translationMemoryName}
                </StyledMetaItem>
              );
            }
            if (item.keyName) {
              parts.push(
                <StyledMetaItem key="key" title={item.keyName}>
                  {item.keyName}
                </StyledMetaItem>
              );
            }
            if (updatedAtLabel) {
              parts.push(
                <StyledMetaItem
                  key="time"
                  title={updatedAtAbsolute}
                  data-cy="translation-tools-translation-memory-item-updated"
                >
                  {updatedAtLabel}
                </StyledMetaItem>
              );
            }
            return parts.flatMap((part, i) =>
              i === 0
                ? [part]
                : [
                    <StyledMetaSeparator key={`sep-${i}`}>
                      ·
                    </StyledMetaSeparator>,
                    part,
                  ]
            );
          })()}
        </StyledMeta>
      </StyledHead>

      <StyledTarget>
        {targetText === '' ? (
          <StyledEmptyText>
            <T keyName="translation_memory_empty" />
          </StyledEmptyText>
        ) : (
          <TranslationWithPlaceholders
            content={targetText}
            locale={languageTag}
            nested={Boolean(pluralVariant)}
            pluralExampleValue={variantExample}
          />
        )}
      </StyledTarget>

      <StyledSource>
        {baseText === '' ? (
          <StyledEmptyText>
            <T keyName="translation_memory_empty" />
          </StyledEmptyText>
        ) : (
          <TranslationWithPlaceholders
            content={baseText}
            locale={baseLanguageTag}
            nested={Boolean(baseVariant)}
            pluralExampleValue={baseVariantExample}
          />
        )}
      </StyledSource>
    </StyledItem>
  );
};
