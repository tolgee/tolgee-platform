import { Box, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

import { useDateFormatter } from 'tg.hooks/useLocale';

const FULL_FORMAT: Intl.DateTimeFormatOptions = {
  dateStyle: 'full',
  timeStyle: 'medium',
};
const TIME_FORMAT: Intl.DateTimeFormatOptions = { timeStyle: 'short' };
const DATE_FORMAT: Intl.DateTimeFormatOptions = { dateStyle: 'medium' };

const RELATIVE_DAYS_LIMIT = 7;

function startOfDay(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

type Props = {
  date: number;
};

/**
 * Recent timestamps read better relative than absolute; the exact value stays in the tooltip
 * because that is what matters when someone is checking whether a session is theirs.
 */
export function SessionDate({ date }: Props) {
  const language = useCurrentLanguage();
  const formatDate = useDateFormatter();

  const value = new Date(date);
  const daysApart = Math.round(
    (startOfDay(value).getTime() - startOfDay(new Date()).getTime()) / 86400000
  );

  return (
    <Tooltip title={formatDate(value, FULL_FORMAT)}>
      <Box component="span">{label()}</Box>
    </Tooltip>
  );

  function label() {
    if (daysApart === 0) {
      return (
        <T
          keyName="session-date-today"
          defaultValue="Today {time}"
          params={{ time: formatDate(value, TIME_FORMAT) }}
        />
      );
    }

    if (daysApart === -1) {
      return (
        <T
          keyName="session-date-yesterday"
          defaultValue="Yesterday {time}"
          params={{ time: formatDate(value, TIME_FORMAT) }}
        />
      );
    }

    if (daysApart > -RELATIVE_DAYS_LIMIT && daysApart < 0) {
      return new Intl.RelativeTimeFormat(language, { numeric: 'auto' }).format(
        daysApart,
        'day'
      );
    }

    return formatDate(value, DATE_FORMAT);
  }
}
