import {
  IconButton,
  InputAdornment,
  InputBaseComponentProps,
  OutlinedInput,
} from '@mui/material';
import { T } from '@tolgee/react';
import copy from 'copy-to-clipboard';
import { Copy06 } from '@untitled-ui/icons-react';

import { useMessage } from 'tg.hooks/useSuccessMessage';

type Props = {
  value: string;
  inputProps?: InputBaseComponentProps;
};

export const ClipboardCopyInput = ({ value, inputProps }: Props) => {
  const messaging = useMessage();
  return (
    <OutlinedInput
      fullWidth
      size="small"
      disabled={true}
      color="success"
      value={value}
      endAdornment={
        <InputAdornment position="end">
          <IconButton
            onClick={() => {
              copy(value || '');
              messaging.success(<T keyName="clipboard_copy_success" />);
            }}
          >
            <Copy06 />
          </IconButton>
        </InputAdornment>
      }
      aria-describedby="outlined-weight-helper-text"
      inputProps={{
        'aria-label': 'weight',
        ...inputProps,
      }}
    />
  );
};
