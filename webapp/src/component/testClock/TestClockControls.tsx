import { FC, useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Chip,
  Divider,
  IconButton,
  TextField as MuiTextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { ClockRefresh } from '@untitled-ui/icons-react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { TOP_BAR_HEIGHT } from 'tg.globalContext/useLayoutService';
import { useTestClockState } from './useTestClockState';
import { DAY, HOUR, MINUTE, SECOND } from './testClockTime';

type Offset = {
  label: string;
  millis?: number;
  months?: number;
  years?: number;
};

const OFFSETS: Offset[] = [
  { label: '30s', millis: 30 * SECOND },
  { label: '1m', millis: MINUTE },
  { label: '5m', millis: 5 * MINUTE },
  { label: '1h', millis: HOUR },
  { label: '1d', millis: DAY },
  { label: '1w', millis: 7 * DAY },
  { label: '1mo', months: 1 },
  { label: '1y', years: 1 },
];

const BACKWARD_OFFSETS = [...OFFSETS].reverse();

const offsetLabel = (offset: Offset, sign: number) =>
  `${sign < 0 ? '−' : '+'}${offset.label}`;

function shiftBy(timestamp: number, offset: Offset, sign: number) {
  if (offset.millis !== undefined) {
    return timestamp + sign * offset.millis;
  }
  const date = new Date(timestamp);
  const day = date.getUTCDate();
  date.setUTCDate(1);
  if (offset.months !== undefined) {
    date.setUTCMonth(date.getUTCMonth() + sign * offset.months);
  }
  if (offset.years !== undefined) {
    date.setUTCFullYear(date.getUTCFullYear() + sign * offset.years);
  }
  const lastDay = new Date(
    Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)
  ).getUTCDate();
  date.setUTCDate(Math.min(day, lastDay));
  return date.getTime();
}

const DELTA_UNITS: [string, number][] = [
  ['d', DAY],
  ['h', HOUR],
  ['m', MINUTE],
  ['s', SECOND],
];

function formatDelta(diff: number) {
  let rest = Math.abs(diff);
  const parts: string[] = [];
  for (const [suffix, size] of DELTA_UNITS) {
    const count = Math.floor(rest / size);
    if (count) {
      parts.push(`${count}${suffix}`);
      rest -= count * size;
    }
    if (parts.length === 2) {
      break;
    }
  }
  return `${diff < 0 ? '-' : '+'}${parts.join(' ') || '0s'}`;
}

const toIso = (timestamp: number) => new Date(timestamp).toISOString();

const parseIso = (value: string) => {
  const parsed = Date.parse(value);
  return isNaN(parsed) ? undefined : parsed;
};

export const useTestClockDateTimeFormatter = () => {
  const formatter = useDateFormatter();
  return (value: number) =>
    formatter(value, {
      timeZone: 'UTC',
      dateStyle: 'short',
      timeStyle: 'long',
    });
};

type Props = {
  /** The staged timestamp; `undefined` stages the current clock time. */
  value?: number;
  onChange?: (value: number | undefined) => void;
};

export const TestClockControls: FC<Props> = (props) => {
  const { t } = useTranslate();
  const formatDateTime = useTestClockDateTimeFormatter();
  const { info, move, reset, isMoving, isResetting } = useTestClockState();

  const [uncontrolledValue, setUncontrolledValue] = useState<
    number | undefined
  >(undefined);
  const controlled = props.onChange !== undefined;
  const stagedValue = controlled ? props.value : uncontrolledValue;
  const setStaged = (value: number | undefined) =>
    controlled ? props.onChange!(value) : setUncontrolledValue(value);

  const currentTimestamp = info?.currentTimestamp;
  const staged = stagedValue ?? currentTimestamp;

  const [text, setText] = useState('');

  useEffect(() => {
    if (staged !== undefined && parseIso(text) !== staged) {
      setText(toIso(staged));
    }
  }, [staged]);

  if (!info || staged === undefined || currentTimestamp === undefined) {
    return null;
  }

  const onTextChange = (value: string) => {
    setText(value);
    const parsed = parseIso(value);
    if (parsed !== undefined) {
      setStaged(parsed);
    }
  };

  const diff = staged - currentTimestamp;

  const renderOffset = (offset: Offset, sign: number) => (
    <Button
      key={offsetLabel(offset, sign)}
      variant="outlined"
      size="small"
      color={sign < 0 ? 'error' : 'success'}
      sx={{ minWidth: 0, px: 1, textTransform: 'none' }}
      onClick={() => setStaged(shiftBy(staged, offset, sign))}
      data-cy="test-clock-offset-button"
      data-cy-offset={offsetLabel(offset, sign)}
    >
      {offsetLabel(offset, sign)}
    </Button>
  );

  return (
    <Box
      data-cy="test-clock-controls"
      sx={{
        position: 'sticky',
        top: TOP_BAR_HEIGHT,
        zIndex: 5,
        display: 'grid',
        gap: 2,
        p: 2,
        border: 1,
        borderColor: 'divider',
        borderRadius: 1,
        backgroundColor: 'background.paper',
      }}
    >
      <Box display="flex" alignItems="center" gap={1}>
        <Typography data-cy="test-clock-current-time">
          {t('test_clock_current_time', 'Current time: {time}', {
            time: formatDateTime(currentTimestamp),
          })}
        </Typography>
        {info.forced ? (
          <Chip
            size="small"
            color="error"
            label={t('test_clock_forced_chip', 'Moved, stands still')}
            data-cy="test-clock-forced-chip"
          />
        ) : (
          <Chip
            size="small"
            label={t('test_clock_real_chip', 'Real time')}
            data-cy="test-clock-real-chip"
          />
        )}
      </Box>

      <Box display="flex" flexWrap="wrap" alignItems="center" gap={0.5}>
        {BACKWARD_OFFSETS.map((offset) => renderOffset(offset, -1))}
        <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
        {OFFSETS.map((offset) => renderOffset(offset, 1))}
      </Box>

      <Box
        display="flex"
        flexWrap="wrap"
        alignItems="center"
        gap={2}
        sx={{ pt: 1, borderTop: 1, borderColor: 'divider' }}
      >
        <Box>
          <Typography data-cy="test-clock-staged-time">
            {formatDateTime(staged)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {diff === 0
              ? t('test_clock_delta_none', 'current time')
              : t('test_clock_delta', '{delta} from current time', {
                  delta: formatDelta(diff),
                })}
          </Typography>
        </Box>
        <Box display="flex" alignItems="center" gap={1} ml="auto">
          <MuiTextField
            size="small"
            sx={{ width: '27ch' }}
            onChange={(e) => onTextChange(e.target.value)}
            value={text}
            data-cy="test-clock-input"
          />
          <Tooltip
            title={t('test_clock_stage_current_time', 'Back to current time')}
          >
            <span>
              <IconButton
                size="small"
                aria-label={t(
                  'test_clock_stage_current_time',
                  'Back to current time'
                )}
                disabled={diff === 0}
                onClick={() => setStaged(undefined)}
                data-cy="test-clock-stage-current-time"
              >
                <ClockRefresh width={18} height={18} />
              </IconButton>
            </span>
          </Tooltip>
          <LoadingButton
            loading={isResetting}
            disabled={!info.forced}
            onClick={() => reset(() => setStaged(undefined))}
            variant="contained"
            color="secondary"
            data-cy="test-clock-reset"
          >
            {t('test_clock_reset_button', 'Reset')}
          </LoadingButton>
          <LoadingButton
            disabled={parseIso(text) !== staged}
            loading={isMoving}
            onClick={() => move(staged, () => setStaged(undefined))}
            color="primary"
            variant="contained"
            data-cy="test-clock-move"
          >
            {t('test_clock_move_button', 'Move clock')}
          </LoadingButton>
        </Box>
      </Box>
    </Box>
  );
};
