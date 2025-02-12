import { useTranslate } from '@tolgee/react';

export function useTimeDistance() {
  const { t } = useTranslate();

  return (timeInPast: number | string | Date) => {
    const differenceInSeconds =
      (Date.now().valueOf() - new Date(timeInPast).valueOf()) / 1000;

    if (differenceInSeconds < 60) {
      return t('time_difference_right_now');
    }

    if (differenceInSeconds < 60 * 60) {
      return t('time_difference_minutes', {
        value: Math.trunc(differenceInSeconds / 60),
      });
    }

    if (differenceInSeconds < 60 * 60 * 24) {
      return t('time_difference_hours', {
        value: Math.trunc(differenceInSeconds / (60 * 60)),
      });
    }

    return t('time_difference_days', {
      value: Math.trunc(differenceInSeconds / (60 * 60 * 24)),
    });
  };
}
