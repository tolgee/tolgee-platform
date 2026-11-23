import { useRef, useState } from 'react';
import {
  Box,
  FormControl,
  MenuItem,
  MenuList,
  Popover,
  Select,
  styled,
  Typography,
} from '@mui/material';
import { DateCalendar, PickersDay, PickersDayProps } from '@mui/x-date-pickers';
import type {} from '@mui/x-date-pickers/AdapterDateFns';
import { Calendar } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';

export type DateRangePreset =
  | 'ALL'
  | 'TODAY'
  | 'YESTERDAY'
  | 'LAST_7_DAYS'
  | 'THIS_MONTH'
  | 'LAST_MONTH'
  | 'CUSTOM';

export type DateRangeValue = {
  preset: DateRangePreset;
  from?: number;
  to?: number;
};

const DAY = 24 * 60 * 60 * 1000;

const monthOf = (time: number) => {
  const d = new Date(time);
  return new Date(d.getFullYear(), d.getMonth(), 1);
};

const shiftMonth = (month: Date, by: number) =>
  new Date(month.getFullYear(), month.getMonth() + by, 1);

const monthStart = (offset: number) => shiftMonth(monthOf(Date.now()), offset);

const startOfDay = (d: Date) =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

/** Inclusive `from`, exclusive `to`, both in epoch ms. */
export const resolveDateRange = (
  value: DateRangeValue
): { from?: number; to?: number } => {
  const now = new Date();
  const today = startOfDay(now);
  switch (value.preset) {
    case 'TODAY':
      return { from: today };
    case 'YESTERDAY':
      return { from: today - DAY, to: today };
    case 'LAST_7_DAYS':
      return { from: today - 6 * DAY };
    case 'THIS_MONTH':
      return { from: new Date(now.getFullYear(), now.getMonth(), 1).getTime() };
    case 'LAST_MONTH':
      return {
        from: new Date(now.getFullYear(), now.getMonth() - 1, 1).getTime(),
        to: new Date(now.getFullYear(), now.getMonth(), 1).getTime(),
      };
    case 'CUSTOM':
      return {
        from: value.from,
        to: value.to !== undefined ? value.to + DAY : undefined,
      };
    default:
      return {};
  }
};

/** URL form: preset name, or `from_to` as yyyy-mm-dd for a custom range. */
export const encodeDateRange = (value: DateRangeValue) => {
  if (value.preset !== 'CUSTOM' || value.from === undefined) {
    return value.preset;
  }
  const iso = (t: number) => {
    const d = new Date(t);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(
      2,
      '0'
    )}-${String(d.getDate()).padStart(2, '0')}`;
  };
  return `${iso(value.from)}_${iso(value.to ?? value.from)}`;
};

export const decodeDateRange = (raw: string | undefined): DateRangeValue => {
  if (!raw) return { preset: 'ALL' };
  const custom = raw.match(/^(\d{4})-(\d{2})-(\d{2})_(\d{4})-(\d{2})-(\d{2})$/);
  if (custom) {
    const [, y1, m1, d1, y2, m2, d2] = custom.map(Number);
    return {
      preset: 'CUSTOM',
      from: new Date(y1, m1 - 1, d1).getTime(),
      to: new Date(y2, m2 - 1, d2).getTime(),
    };
  }
  return { preset: raw as DateRangePreset };
};

const StyledSelect = styled(Select)`
  width: 200px;
  height: 40px;
  & .MuiSelect-select {
    display: flex;
    align-items: center;
    cursor: pointer;
  }
  & .MuiSelect-icon {
    width: 20px;
    height: 20px;
    top: calc(50% - 10px);
    right: 10px;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

const StyledValue = styled(Typography)`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin: -2px 0px;
`;

const StyledPanel = styled(Box)`
  display: flex;
`;

const StyledPresets = styled(MenuList)`
  min-width: 200px;
  border-right: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const StyledCalendars = styled(Box)`
  display: flex;
  & .MuiDateCalendar-root:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

type RangeDayProps = PickersDayProps<Date> & {
  rangeFrom?: number;
  rangeTo?: number;
};

const RangeDay = ({ rangeFrom, rangeTo, ...props }: RangeDayProps) => {
  const time = startOfDay(props.day);
  const isEdge = time === rangeFrom || time === rangeTo;
  const inside =
    rangeFrom !== undefined &&
    rangeTo !== undefined &&
    time > rangeFrom &&
    time < rangeTo;
  return (
    <PickersDay
      {...props}
      selected={isEdge}
      sx={
        inside
          ? {
              backgroundColor: (theme) => theme.palette.primary.light + '33',
              borderRadius: 0,
            }
          : undefined
      }
    />
  );
};

type Props = {
  value: DateRangeValue;
  onChange: (value: DateRangeValue) => void;
  since?: number;
  maxRangeDays?: number;
};

const DAY_MS = 24 * 60 * 60 * 1000;

export const ActivityDateRangeFilter = ({
  value,
  onChange,
  since,
  maxRangeDays,
}: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [showCalendar, setShowCalendar] = useState(false);
  const triggerRef = useRef<HTMLDivElement>(null);
  const [draft, setDraft] = useState<{ from?: number; to?: number }>({});
  const [leftMonth, setLeftMonth] = useState(() => monthStart(-1));
  const [rightMonth, setRightMonth] = useState(() => monthStart(0));

  const presets: { value: DateRangePreset; label: string }[] = [
    { value: 'ALL', label: t('activity_date_all_time', 'All time') },
    { value: 'TODAY', label: t('activity_date_today', 'Today') },
    { value: 'YESTERDAY', label: t('activity_date_yesterday', 'Yesterday') },
    {
      value: 'LAST_7_DAYS',
      label: t('activity_date_last_7_days', 'Last 7 days'),
    },
    { value: 'THIS_MONTH', label: t('activity_date_this_month', 'This month') },
    { value: 'LAST_MONTH', label: t('activity_date_last_month', 'Last month') },
    { value: 'CUSTOM', label: t('activity_date_custom', 'Custom range') },
  ];

  const label =
    value.preset === 'CUSTOM' && value.from !== undefined
      ? `${formatDate(value.from)} – ${formatDate(value.to ?? value.from)}`
      : presets.find((p) => p.value === value.preset)?.label;

  const open = () => {
    setAnchorEl(triggerRef.current);
    setShowCalendar(value.preset === 'CUSTOM');
    setDraft({ from: value.from, to: value.to });
    if (value.preset === 'CUSTOM' && value.from !== undefined) {
      const current = monthStart(0).getTime();
      const left = monthOf(value.from);
      let right = monthOf(value.to ?? value.from);
      if (right.getTime() <= left.getTime()) {
        right = shiftMonth(left, 1);
      }
      if (right.getTime() > current) {
        right = new Date(current);
      }
      setLeftMonth(
        left.getTime() < right.getTime() ? left : shiftMonth(right, -1)
      );
      setRightMonth(right);
    }
  };

  const close = () => {
    setAnchorEl(null);
    setShowCalendar(false);
  };

  const pickDay = (day: Date | null) => {
    if (!day) return;
    const time = startOfDay(day);
    if (draft.from === undefined || draft.to !== undefined) {
      setDraft({ from: time });
      return;
    }
    const from = Math.min(draft.from, time);
    const to = Math.max(draft.from, time);
    setDraft({ from, to });
    onChange({ preset: 'CUSTOM', from, to });
  };

  const sinceDate =
    since !== undefined ? new Date(startOfDay(new Date(since))) : undefined;
  const isOutOfRange = (day: Date) => {
    if (sinceDate && day.getTime() < sinceDate.getTime()) return true;
    if (!maxRangeDays || draft.from === undefined || draft.to !== undefined)
      return false;
    return Math.abs(startOfDay(day) - draft.from) > maxRangeDays * DAY_MS;
  };

  const dayProps = { rangeFrom: draft.from, rangeTo: draft.to ?? draft.from };

  return (
    <>
      <FormControl
        ref={triggerRef}
        variant="outlined"
        size="small"
        focused={Boolean(anchorEl)}
        data-cy="activity-stream-filter-time"
      >
        <StyledSelect
          value={value.preset}
          open={false}
          onOpen={open}
          IconComponent={Calendar}
          renderValue={() => (
            <StyledValue color="textPrimary" variant="body1">
              {label}
            </StyledValue>
          )}
        >
          <MenuItem value={value.preset}>{label}</MenuItem>
        </StyledSelect>
      </FormControl>
      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={close}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <StyledPanel>
          <StyledPresets>
            {presets.map((preset) => (
              <MenuItem
                key={preset.value}
                selected={
                  preset.value === 'CUSTOM'
                    ? showCalendar
                    : !showCalendar && value.preset === preset.value
                }
                onClick={() => {
                  if (preset.value === 'CUSTOM') {
                    setShowCalendar(true);
                    return;
                  }
                  onChange({ preset: preset.value });
                  close();
                }}
                data-cy="activity-date-preset"
                data-cy-preset={preset.value}
              >
                {preset.label}
              </MenuItem>
            ))}
          </StyledPresets>
          {showCalendar && (
            <StyledCalendars data-cy="activity-date-calendars">
              <DateCalendar
                key={`a-${leftMonth.getTime()}-${rightMonth.getTime()}`}
                referenceDate={leftMonth}
                value={null}
                onChange={(day, state) => state === 'finish' && pickDay(day)}
                onMonthChange={(m) => setLeftMonth(monthOf(m.getTime()))}
                onYearChange={(y) => setLeftMonth(monthOf(y.getTime()))}
                maxDate={new Date(rightMonth.getTime() - 1)}
                minDate={sinceDate}
                shouldDisableDate={isOutOfRange}
                views={['year', 'month', 'day']}
                openTo="day"
                showDaysOutsideCurrentMonth
                disableFuture
                slots={{ day: RangeDay }}
                slotProps={{ day: dayProps as never }}
              />
              <DateCalendar
                key={`b-${rightMonth.getTime()}-${leftMonth.getTime()}`}
                referenceDate={rightMonth}
                value={null}
                onChange={(day, state) => state === 'finish' && pickDay(day)}
                onMonthChange={(m) => setRightMonth(monthOf(m.getTime()))}
                onYearChange={(y) => setRightMonth(monthOf(y.getTime()))}
                minDate={shiftMonth(leftMonth, 1)}
                shouldDisableDate={isOutOfRange}
                views={['year', 'month', 'day']}
                openTo="day"
                showDaysOutsideCurrentMonth
                disableFuture
                slots={{ day: RangeDay }}
                slotProps={{ day: dayProps as never }}
              />
            </StyledCalendars>
          )}
        </StyledPanel>
      </Popover>
    </>
  );
};
