import React, { useEffect, useState } from 'react';
import {
  Box,
  IconButton,
  InputAdornment,
  styled,
  TextField,
  Tooltip,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { RefreshCcw01 } from '@untitled-ui/icons-react';

const DefaultChip = styled('button')`
  border: 1px dashed ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.emphasis[50]};
  color: ${({ theme }) => theme.palette.text.secondary};
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  &:hover {
    border-color: ${({ theme }) => theme.palette.primary.main};
    color: ${({ theme }) => theme.palette.primary.main};
  }
  &:disabled {
    cursor: default;
    opacity: 0.6;
  }
`;

const ReadOnlyChip = styled('span')`
  display: inline-block;
  border: 1px dashed ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.emphasis[50]};
  color: ${({ theme }) => theme.palette.text.secondary};
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  white-space: nowrap;
  cursor: default;
`;

const OverrideWrap = styled(Box)`
  display: inline-flex;
  align-items: center;
  gap: 2px;
`;

type Props = {
  defaultPenalty: number;
  override: number | null | undefined;
  onChange: (value: number | null) => void;
  disabled?: boolean;
  readOnly?: boolean;
  dataCy?: string;
};

const validatePenalty = (
  raw: string
): { value: number | null; error: boolean } => {
  const trimmed = raw.trim();
  if (trimmed === '') return { value: null, error: false };
  if (!/^\d+$/.test(trimmed)) return { value: null, error: true };
  const n = parseInt(trimmed, 10);
  if (n < 0 || n > 100) return { value: n, error: true };
  return { value: n, error: false };
};

export const PenaltyCell: React.VFC<Props> = ({
  defaultPenalty,
  override,
  onChange,
  disabled,
  readOnly,
  dataCy,
}) => {
  const { t } = useTranslate();
  const [editingInherit, setEditingInherit] = useState(false);
  const isOverride = override !== null && override !== undefined;
  const initial = isOverride ? String(override) : String(defaultPenalty);
  const [draft, setDraft] = useState(initial);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (isOverride) setDraft(String(override));
  }, [override, isOverride]);

  if (disabled) {
    return (
      <DefaultChip disabled data-cy={dataCy}>
        0%
      </DefaultChip>
    );
  }

  if (readOnly) {
    const label = isOverride
      ? `${override}%`
      : t('translation_memory_penalty_default_chip', 'default · {value}%', {
          value: defaultPenalty,
        });
    return <ReadOnlyChip data-cy={dataCy}>{label}</ReadOnlyChip>;
  }

  if (!isOverride && !editingInherit) {
    return (
      <Tooltip
        title={t(
          'translation_memory_penalty_default_chip_tooltip',
          'Inherits TM default. Click to override.'
        )}
      >
        <DefaultChip
          type="button"
          onClick={() => {
            setDraft(String(defaultPenalty));
            setEditingInherit(true);
          }}
          data-cy={dataCy}
        >
          {t('translation_memory_penalty_default_chip', 'default · {value}%', {
            value: defaultPenalty,
          })}
        </DefaultChip>
      </Tooltip>
    );
  }

  return (
    <OverrideWrap>
      <TextField
        size="small"
        autoFocus={editingInherit}
        value={draft}
        onChange={(e) => {
          const next = e.target.value;
          setDraft(next);
          const parsed = validatePenalty(next);
          setError(parsed.error);
          // Commit on every valid keystroke so a subsequent click on Save
          // doesn't race React's async state flush of an onBlur commit.
          if (!parsed.error) {
            onChange(parsed.value);
          }
        }}
        error={error}
        inputProps={{
          inputMode: 'numeric',
          maxLength: 3,
          'data-cy': dataCy,
        }}
        sx={{
          width: 78,
          '& .MuiInputBase-root': {
            fontSize: 13,
          },
        }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end" sx={{ mr: -0.5 }}>
              %
            </InputAdornment>
          ),
        }}
      />
      <Tooltip
        title={t(
          'translation_memory_penalty_reset_tooltip',
          'Reset to TM default'
        )}
      >
        <IconButton
          size="small"
          onClick={() => {
            setEditingInherit(false);
            setError(false);
            setDraft(String(defaultPenalty));
            onChange(null);
          }}
        >
          <RefreshCcw01 width={14} height={14} />
        </IconButton>
      </Tooltip>
    </OverrideWrap>
  );
};
