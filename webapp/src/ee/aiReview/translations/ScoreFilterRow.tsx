import { MutableRefObject, useEffect, useRef, useState } from 'react';
import { useDebounce } from 'use-debounce';
import { useTranslate } from '@tolgee/react';
import { MenuItem, MenuItemProps, Select, TextField } from '@mui/material';

import {
  AiReviewScoreFilterMode,
  FiltersInternal,
  withoutAiReviewScoreFilter,
  withoutNotReviewedYet,
} from 'tg.views/projects/translations/TranslationFilters/tools';

export const ScoreFilterRow = ({
  value,
  setFilters,
  flushRef,
  ...menuItemProps
}: {
  value: FiltersInternal;
  setFilters: (value: FiltersInternal) => void;
  /** The parent calls it from the submenu's onClose, before the row unmounts. */
  flushRef: MutableRefObject<(() => void) | undefined>;
} & Omit<MenuItemProps, 'value'>) => {
  const { t } = useTranslate();
  const selectRef = useRef<HTMLInputElement>(null);
  const localModeEdit = useRef(false);
  const [scoreInput, setScoreInput] = useState(value.filterAiReviewScore);
  const [scoreInputDebounced] = useDebounce(scoreInput, 500);

  // An external mode change (for example browser Back) invalidates the draft.
  // A mode picked in this row keeps it, so the typed value commits under the
  // new comparison.
  useEffect(() => {
    if (localModeEdit.current) {
      localModeEdit.current = false;
      return;
    }
    setScoreInput(value.filterAiReviewScore);
  }, [value.filterAiReviewScore, value.filterAiReviewScoreMode]);

  useEffect(() => {
    if (value.filterAiReviewScoreMode === undefined) {
      return;
    }
    if (scoreInputDebounced !== value.filterAiReviewScore) {
      setFilters(commitAiReviewScore(value, scoreInputDebounced));
    }
  }, [scoreInputDebounced]);

  // Closing the menu unmounts the row before the debounce fires, which would
  // silently drop the typed value. The parent flushes the draft from the
  // submenu's onClose; a route change never calls it, so the draft cannot be
  // written onto another page's URL state.
  flushRef.current = () => {
    if (value.filterAiReviewScoreMode === undefined) {
      return;
    }
    if (scoreInput !== value.filterAiReviewScore) {
      setFilters(commitAiReviewScore(value, scoreInput));
    }
  };

  const scoreModes: {
    mode: 'ALL' | AiReviewScoreFilterMode;
    label: string;
  }[] = [
    { mode: 'ALL', label: t('ai_review_filter_score_all', 'All scores') },
    {
      mode: 'HIGHER_THAN',
      label: t('ai_review_filter_score_higher_than', 'Higher than'),
    },
    {
      mode: 'LOWER_THAN',
      label: t('ai_review_filter_score_lower_than', 'Lower than'),
    },
    {
      mode: 'EQUAL_TO',
      label: t('ai_review_filter_score_equal_to', 'Equal to'),
    },
  ];

  return (
    /* MUI Menu keeps keyboard focus on menu items, so the row itself must
       be a MenuItem; Enter or ArrowRight then moves focus into the controls. */
    <MenuItem
      {...menuItemProps}
      disableRipple
      data-cy="ai-review-filter-score-row"
      sx={{
        gap: 1,
        paddingX: 2,
        paddingY: 0.5,
        cursor: 'default',
        '&:hover': { backgroundColor: 'transparent' },
      }}
      onKeyDown={(e) => {
        if (e.target === e.currentTarget) {
          if (e.key === 'Enter' || e.key === 'ArrowRight') {
            e.preventDefault();
            e.stopPropagation();
            selectRef.current?.focus();
          }
          return;
        }
        if (e.key !== 'Escape') {
          e.stopPropagation();
        }
      }}
    >
      <Select
        size="small"
        fullWidth
        inputRef={selectRef}
        value={value.filterAiReviewScoreMode ?? 'ALL'}
        onChange={(e) => {
          const mode =
            e.target.value === 'ALL'
              ? undefined
              : (e.target.value as AiReviewScoreFilterMode);
          if (mode === undefined) {
            setScoreInput(undefined);
          } else if (value.filterAiReviewScoreMode === undefined) {
            setScoreInput(value.filterAiReviewScore);
          }
          localModeEdit.current = mode !== undefined;
          setFilters(setAiReviewScoreMode(value, mode));
        }}
        inputProps={{
          'aria-label': t(
            'ai_review_filter_score_mode_label',
            'Score comparison'
          ),
        }}
        SelectDisplayProps={
          {
            'data-cy': 'ai-review-filter-score-mode',
          } as any
        }
      >
        {scoreModes.map(({ mode, label }) => (
          <MenuItem
            key={mode}
            value={mode}
            data-cy="ai-review-filter-score-mode-item"
            data-cy-mode={mode}
          >
            {label}
          </MenuItem>
        ))}
      </Select>
      <TextField
        size="small"
        type="number"
        disabled={value.filterAiReviewScoreMode === undefined}
        value={scoreInput ?? ''}
        onChange={(e) =>
          setScoreInput(
            e.target.value === ''
              ? undefined
              : clampScore(Number(e.target.value))
          )
        }
        inputProps={{
          'data-cy': 'ai-review-filter-score-input',
          'aria-label': t(
            'ai_review_filter_score_threshold_label',
            'Score threshold'
          ),
          min: 0,
          max: 100,
        }}
        sx={(theme) => ({
          width: theme.spacing(10),
          '& input[type=number]': { MozAppearance: 'textfield' },
          '& input[type=number]::-webkit-outer-spin-button, & input[type=number]::-webkit-inner-spin-button':
            { WebkitAppearance: 'none', margin: 0 },
        })}
      />
    </MenuItem>
  );
};

export function setAiReviewScoreMode(
  value: FiltersInternal,
  mode: AiReviewScoreFilterMode | undefined
): FiltersInternal {
  if (mode === undefined) {
    return withoutAiReviewScoreFilter(value);
  }
  return { ...value, filterAiReviewScoreMode: mode };
}

export function commitAiReviewScore(
  value: FiltersInternal,
  score: number | undefined
): FiltersInternal {
  const next = setAiReviewScoreValue(value, score);
  if (score === undefined) {
    return next;
  }
  return withoutNotReviewedYet(next);
}

export function setAiReviewScoreValue(
  value: FiltersInternal,
  score: number | undefined
): FiltersInternal {
  if (score === undefined) {
    return { ...value, filterAiReviewScore: undefined };
  }
  return { ...value, filterAiReviewScore: clampScore(score) };
}

const clampScore = (score: number) =>
  Math.min(100, Math.max(0, Math.round(score)));
