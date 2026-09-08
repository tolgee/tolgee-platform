import React from 'react';
import { Box, Button, DialogActions, DialogContent } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppScopeChips } from 'tg.component/apps/AppScopeChips';
import { AppManifestPreviewModel } from 'tg.component/apps/useAppRegisterState';

type Props = {
  preview: AppManifestPreviewModel;
  intro: React.ReactNode;
  noScopesLabel: React.ReactNode;
  backLabel: React.ReactNode;
  submitLabel: React.ReactNode;
  loading: boolean;
  errorNode?: React.ReactNode;
  onBack: () => void;
  onSubmit: () => void;
  contentDataCy: string;
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
  errorNode,
  onBack,
  onSubmit,
  contentDataCy,
  scopesDataCy,
  backDataCy,
  submitDataCy,
}: Props) => (
  <>
    <DialogContent data-cy={contentDataCy}>
      {errorNode}
      <Box mb={2}>
        <AppSummary
          name={preview.name}
          version={preview.version}
          url={preview.baseUrl}
          icon={preview.icon}
          appId={preview.appId}
        />
      </Box>

      <AppScopeChips
        stacked
        scopes={preview.requestedScopes}
        dataCy={scopesDataCy}
        label={intro}
        emptyLabel={noScopesLabel}
      />
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
