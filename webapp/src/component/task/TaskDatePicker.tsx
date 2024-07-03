import { ComponentProps } from 'react';
import { DatePicker } from '@mui/x-date-pickers';

import { TextField as NonFormTextField } from 'tg.component/common/TextField';

const DueDatePicker = (props: ComponentProps<typeof NonFormTextField>) => {
  return <NonFormTextField {...props} />;
};

type Props = {
  value: number | null;
  onChange: (value: number | null) => void;
  label: React.ReactNode;
};

export const TaskDatePicker = ({ value, onChange, label }: Props) => {
  return (
    <DatePicker
      value={value ? new Date(value) : null}
      onChange={(value) => {
        if (value) {
          const year = value.getFullYear();
          const month = value.getMonth(); // getMonth() returns a zero-based month (0-11)
          const day = value.getDate();

          // Create a new Date object in UTC using the extracted values
          const utcDate = Date.UTC(year, month, day);

          onChange(utcDate);
        } else {
          onChange(null);
        }
      }}
      slotProps={{
        textField: { label: label },
        actionBar: { actions: ['clear', 'accept'] },
      }}
      slots={{
        textField: DueDatePicker,
      }}
    />
  );
};
