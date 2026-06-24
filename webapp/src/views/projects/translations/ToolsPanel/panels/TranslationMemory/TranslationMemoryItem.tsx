import { styled, Tooltip } from '@mui/material';
import { KeyName } from 'tg.component/KeyName/KeyName';
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

const StyledItem = styled('div')`
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: ${({ theme }) => theme.spacing(1.5, 2)};
  transition: background 0.1s ease-in-out, color 0.1s ease-in-out;

  /* Inset divider — aligned with the text's horizontal extent on both sides instead of
     hugging the panel's full width. */
  &::after {
    content: '';
    position: absolute;
    left: ${({ theme }) => theme.spacing(2)};
    right: ${({ theme }) => theme.spacing(2)};
    bottom: 0;
    height: 1px;
    background: ${({ theme }) => theme.palette.divider1};
  }

  &:last-of-type::after {
    content: none;
  }
  &.clickable {
    cursor: pointer;
  }
  &.clickable:hover {
    background: ${({ theme }) => theme.palette.tokens.text._states.selected};
  }
  /* Mirror MachineTranslationItem: clicking inserts the suggested text, so the suggestion
     itself turns primary on hover to advertise the action. */
  &.clickable:hover .tm-suggestion-target {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledHead = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
`;

// Three-tier scheme matching the pre-PR design (green / orange / grey at 1.0 / 0.7
// thresholds), expressed via theme tokens. 100% = perfect match, 70%+ = fuzzy but usable,
// below 70% = weak. Penalised matches add a yellow ring + dot inside the pill.
const StyledScore = styled('div')`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 9999px;
  font-variant-numeric: tabular-nums;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.4px;
  white-space: nowrap;

  &.s-100 {
    color: ${({ theme }) =>
      theme.palette.tokens._components.alert.success.color};
    background: ${({ theme }) =>
      theme.palette.tokens._components.alert.success.background};
  }
  &.s-mid {
    color: ${({ theme }) =>
      theme.palette.tokens._components.alert.warning.color};
    background: ${({ theme }) =>
      theme.palette.tokens._components.alert.warning.background};
  }
  &.s-low {
    color: ${({ theme }) => theme.palette.text.secondary};
    background: ${({ theme }) => theme.palette.emphasis[100]};
  }
  &.penalized {
    border: 1px solid ${({ theme }) => theme.palette.tokens.warning.main};
    padding: 3px 7px;
  }
`;

const StyledScoreDot = styled('span')`
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: ${({ theme }) => theme.palette.tokens.warning.dark};
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

// The meta line has three parts of unequal importance. Rather than letting them
// all shrink uniformly (which collapses every part to a useless "D…" stub), only
// the key reference yields space — it is the longest and noisiest. The TM name
// and timestamp keep their natural width; each still ellipsis-truncates on its
// own as a last resort so a pathological value can't break the line.
const StyledMetaTmName = styled('span')`
  flex-shrink: 0;
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledMetaKey = styled('span')`
  flex-shrink: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledMetaTime = styled('span')`
  flex-shrink: 0;
  white-space: nowrap;
`;

const StyledMetaSeparator = styled('span')`
  flex-shrink: 0;
  opacity: 0.6;
`;

const StyledTarget = styled('div')`
  font-size: 15px;
  color: ${({ theme }) => theme.palette.text.primary};
  overflow-wrap: break-word;
  overflow: hidden;
  transition: color 0.1s ease-in-out;
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
  if (similarityPercent >= 70) return 's-mid';
  return 's-low';
};

// Stable test contract — separate from the styling class so e2e tests can assert tier
// without coupling to the visual class name. `penalized` overrides any base tier when set.
const tierAttr = (similarityPercent: number, penalized: boolean): string => {
  if (penalized) return 'penalized';
  if (similarityPercent >= 100) return '100';
  if (similarityPercent >= 70) return 'mid';
  return 'low';
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
      data-tier={tierAttr(displayedPercent, penalty > 0)}
      data-cy="translation-tools-translation-memory-item-score"
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
                defaultValue="{raw}% similarity − {penalty}% penalty"
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
                <Tooltip key="tm" title={item.translationMemoryName}>
                  <StyledMetaTmName data-cy="translation-tools-translation-memory-item-tm-name">
                    {item.translationMemoryName}
                  </StyledMetaTmName>
                </Tooltip>
              );
            }
            if (item.keyName) {
              parts.push(
                <Tooltip key="key" title={item.keyName}>
                  <StyledMetaKey data-cy="translation-tools-translation-memory-item-key-name">
                    <KeyName name={item.keyName} />
                  </StyledMetaKey>
                </Tooltip>
              );
            }
            if (updatedAtLabel) {
              parts.push(
                <Tooltip key="time" title={updatedAtAbsolute ?? ''}>
                  <StyledMetaTime data-cy="translation-tools-translation-memory-item-updated">
                    {updatedAtLabel}
                  </StyledMetaTime>
                </Tooltip>
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

      <StyledTarget className="tm-suggestion-target">
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
