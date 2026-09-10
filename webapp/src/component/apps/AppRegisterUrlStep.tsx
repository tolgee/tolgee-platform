import React from 'react';
import {
  Box,
  Button,
  DialogActions,
  DialogContent,
  Link,
  TextField,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import { DOCS_LINKS } from 'tg.constants/docLinks';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { AppsAlphaBanner } from 'tg.component/apps/AppsAlphaBadge';

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onCancel: () => void;
  loading: boolean;
  description?: React.ReactNode;
  errorNode?: React.ReactNode;
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
  errorNode,
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
      <Box mb={2}>
        <AppsAlphaBanner />
      </Box>
      {description && <Box mb={1}>{description}</Box>}
      {errorNode}
      <Typography variant="body2" color="text.secondary" mb={2}>
        <T
          keyName="apps_register_manifest_url_info"
          defaultValue="The manifest URL comes from the app you are connecting. Apps built with the Tolgee apps SDK serve it at /manifest.json."
        />{' '}
        <Link
          href={DOCS_LINKS.apps}
          target="_blank"
          rel="noreferrer noopener"
          data-cy="apps-register-docs-link"
        >
          <T
            keyName="apps_register_docs_link"
            defaultValue="Learn more in the documentation."
          />
        </Link>
      </Typography>
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
