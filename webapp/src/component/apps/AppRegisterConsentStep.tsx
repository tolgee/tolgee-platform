import React from 'react';
import {
  Box,
  Button,
  DialogActions,
  DialogContent,
  Typography,
} from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppChips } from 'tg.component/apps/AppChips';
import { AppManifestPreviewModel } from 'tg.component/apps/useAppRegisterState';

type Props = {
  preview: AppManifestPreviewModel;
  intro: React.ReactNode;
  noScopesLabel: React.ReactNode;
  backLabel: React.ReactNode;
  submitLabel: React.ReactNode;
  loading: boolean;
  onBack: () => void;
  onSubmit: () => void;
  contentDataCy: string;
  noScopesDataCy: string;
  scopesDataCy: string;
  backDataCy: string;
  submitDataCy: string;
};

/** Second registration step: what the fetched manifest asks for, before it is approved. */
export const AppRegisterConsentStep = ({
  preview,
  intro,
  noScopesLabel,
  backLabel,
  submitLabel,
  loading,
  onBack,
  onSubmit,
  contentDataCy,
  noScopesDataCy,
  scopesDataCy,
  backDataCy,
  submitDataCy,
}: Props) => (
  <>
    <DialogContent data-cy={contentDataCy}>
      <Box mb={2}>
        <AppSummary
          name={preview.name}
          version={preview.version}
          url={preview.baseUrl}
        />
      </Box>

      <Typography variant="body2" mb={1}>
        {intro}
      </Typography>

      {preview.requestedScopes.length === 0 && (
        <Typography
          variant="body2"
          color="text.secondary"
          data-cy={noScopesDataCy}
        >
          {noScopesLabel}
        </Typography>
      )}

      <AppChips items={preview.requestedScopes} dataCy={scopesDataCy} />
    </DialogContent>
    <DialogActions>
      <Button data-cy={backDataCy} onClick={onBack} disabled={loading}>
        {backLabel}
      </Button>
      <LoadingButton
        data-cy={submitDataCy}
        variant="contained"
        color="primary"
        loading={loading}
        onClick={onSubmit}
      >
        {submitLabel}
      </LoadingButton>
    </DialogActions>
  </>
);
