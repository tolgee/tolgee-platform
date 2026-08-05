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
const MS_PER_DAY = 86400000;

function startOfDay(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

/** Standalone in a table cell, so it reads as a sentence start even where the locale lowercases. */
function capitalize(value: string, language: string) {
  return value.charAt(0).toLocaleUpperCase(language) + value.slice(1);
}

type Props = {
  date: number;
};

/**
 * Recent timestamps read better relative than absolute; the exact value stays in the tooltip
 * because that is what matters when someone is checking whether a session is theirs.
 */
export function SessionDate({ date }: Props) {
  const language = useCurrentLanguage() ?? 'en';
  const formatDate = useDateFormatter();

  const value = new Date(date);
  const daysApart = Math.round(
    (startOfDay(value).getTime() - startOfDay(new Date()).getTime()) /
      MS_PER_DAY
  );

  return (
    <Tooltip title={formatDate(value, FULL_FORMAT)}>
      <Box component="span">{label()}</Box>
    </Tooltip>
  );

  function label() {
    if (daysApart <= -RELATIVE_DAYS_LIMIT || daysApart > 0) {
      return formatDate(value, DATE_FORMAT);
    }

    // `numeric: 'auto'` is what turns -1 into the locale's "yesterday" rather than "1 day ago".
    const relative = capitalize(
      new Intl.RelativeTimeFormat(language, { numeric: 'auto' }).format(
        daysApart,
        'day'
      ),
      language
    );

    // "3 days ago" already implies imprecision, so a clock time only adds noise.
    if (daysApart < -1) {
      return relative;
    }

    return (
      <T
        keyName="session-date-relative"
        defaultValue="{day} {time}"
        params={{ day: relative, time: formatDate(value, TIME_FORMAT) }}
      />
    );
  }
}
