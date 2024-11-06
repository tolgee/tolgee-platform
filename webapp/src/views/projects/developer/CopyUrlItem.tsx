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
  maxWidth?: number;
};

export const CopyUrlItem = ({ value, inputProps, maxWidth = 350 }: Props) => {
  const messaging = useMessage();
  return (
    <OutlinedInput
      sx={{ maxWidth, minWidth: 100 }}
      size="small"
      readOnly
      fullWidth
      disabled
      endAdornment={
        <InputAdornment position="end">
          <IconButton
            size="small"
            onClick={() => {
              copy(value || '');
              messaging.success(<T keyName="clipboard_copy_success" />);
            }}
          >
            <Copy06 />
          </IconButton>
        </InputAdornment>
      }
      value={value}
    />
  );
};
