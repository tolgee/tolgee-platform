import { Tooltip } from '@mui/material';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

export const FormatedDateTooltip = ({ date }: { date: number }) => {
  const formatDate = useDateFormatter();
  const distance = useTimeDistance();
  return (
    <Tooltip
      title={formatDate(date, {
        dateStyle: 'long',
        timeStyle: 'short',
      })}
    >
      <span>{distance(date)}</span>
    </Tooltip>
  );
};
