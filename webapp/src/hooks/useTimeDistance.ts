import { useTranslate } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useCurrentDate } from 'tg.hooks/useCurrentDate';

export function useTimeDistance() {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const currentDate = useCurrentDate();

  return (timeInPast: number | string | Date) => {
    const diffSeconds =
      (currentDate.valueOf() - new Date(timeInPast).valueOf()) / 1000;
    const isFuture = diffSeconds < 0;
    const differenceInSeconds = Math.abs(diffSeconds);

    if (differenceInSeconds < 60) {
      return isFuture
        ? t('time_difference_in_seconds', {
            value: Math.trunc(differenceInSeconds),
          })
        : t('time_difference_right_now');
    }

    if (differenceInSeconds < 60 * 60) {
      const value = Math.trunc(differenceInSeconds / 60);
      return isFuture
        ? t('time_difference_in_minutes', { value })
        : t('time_difference_minutes', { value });
    }

    if (differenceInSeconds < 60 * 60 * 24) {
      const value = Math.trunc(differenceInSeconds / (60 * 60));
      return isFuture
        ? t('time_difference_in_hours', { value })
        : t('time_difference_hours', { value });
    }

    if (differenceInSeconds < 60 * 60 * 24 * 30) {
      const value = Math.trunc(differenceInSeconds / (60 * 60 * 24));
      return isFuture
        ? t('time_difference_in_days', { value })
        : t('time_difference_days', { value });
    }

    return formatDate(new Date(timeInPast), { dateStyle: 'medium' });
  };
}
