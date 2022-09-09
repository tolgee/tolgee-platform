import { IconButton, InputAdornment, OutlinedInput } from '@mui/material';
import copy from 'copy-to-clipboard';
import { ContentCopy } from '@mui/icons-material';

export const ClipboardCopyInput = (props: { value: string }) => (
  <OutlinedInput
    fullWidth
    size="small"
    disabled={true}
    color="success"
    value={props.value}
    endAdornment={
      <InputAdornment position="end">
        <IconButton onClick={() => copy(props.value || '')}>
          <ContentCopy />
        </IconButton>
      </InputAdornment>
    }
    aria-describedby="outlined-weight-helper-text"
    inputProps={{
      'aria-label': 'weight',
    }}
  />
);
