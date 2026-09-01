import React from 'react';
import {
  Box,
  Button,
  DialogActions,
  DialogContent,
  TextField,
} from '@mui/material';
import { T } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onCancel: () => void;
  loading: boolean;
  description?: React.ReactNode;
  fieldLabel: string;
  fieldHelperText: string;
  submitLabel: React.ReactNode;
  fieldDataCy: string;
  submitDataCy: string;
};

/** First registration step: where the caller's manifest URL is entered. */
export const AppRegisterUrlStep = ({
  value,
  onChange,
  onSubmit,
  onCancel,
  loading,
  description,
  fieldLabel,
  fieldHelperText,
  submitLabel,
  fieldDataCy,
  submitDataCy,
}: Props) => (
  <form
    onSubmit={(event) => {
      event.preventDefault();
      onSubmit();
    }}
  >
    <DialogContent>
      {description && <Box mb={1}>{description}</Box>}
      <TextField
        data-cy={fieldDataCy}
        label={fieldLabel}
        helperText={fieldHelperText}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        fullWidth
        autoFocus
        margin="normal"
        placeholder="https://my-plugin.example.com/manifest.json"
      />
    </DialogContent>
    <DialogActions>
      <Button onClick={onCancel}>
        <T keyName="global_cancel_button" defaultValue="Cancel" />
      </Button>
      <LoadingButton
        data-cy={submitDataCy}
        type="submit"
        variant="contained"
        color="primary"
        loading={loading}
        disabled={!value}
      >
        {submitLabel}
      </LoadingButton>
    </DialogActions>
  </form>
);
