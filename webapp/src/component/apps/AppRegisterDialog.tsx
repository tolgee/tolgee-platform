import React, { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';

import { AppSummary } from './AppSummary';
import { AppChips } from './AppChips';

type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];

/**
 * Every value must be a string literal at the call site — the tooling that types
 * `gcy()` scans the source for `*DataCy` literals and cannot follow a computed one.
 */
export type AppRegisterDialogDataCy = {
  dialogDataCy: string;
  manifestUrlDataCy: string;
  continueDataCy: string;
  consentDataCy: string;
  noScopesDataCy: string;
  scopesDataCy: string;
  backDataCy: string;
  submitDataCy: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  description?: React.ReactNode;
  previewLoading: boolean;
  registerLoading: boolean;
  onPreview: (
    manifestUrl: string,
    onSuccess: (preview: AppManifestPreviewModel) => void
  ) => void;
  onRegister: (manifestUrl: string, onSuccess: () => void) => void;
  dataCy: AppRegisterDialogDataCy;
};

/**
 * The manifest URL → consent → register flow, shared by the organization and the
 * server administration screens. The caller owns the API calls, so the same UI can
 * register an app for an organization or natively for the whole server.
 */
export const AppRegisterDialog = ({
  open,
  onClose,
  description,
  previewLoading,
  registerLoading,
  onPreview,
  onRegister,
  dataCy,
}: Props) => {
  const { t } = useTranslate();

  const [manifestUrl, setManifestUrl] = useState('');
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);

  const handleClose = () => {
    setManifestUrl('');
    setPreview(null);
    onClose();
  };

  const handlePreview = (event: React.FormEvent) => {
    event.preventDefault();
    if (!manifestUrl) return;
    onPreview(manifestUrl, setPreview);
  };

  const handleRegister = () => {
    if (!manifestUrl) return;
    onRegister(manifestUrl, handleClose);
  };

  const step: 'url' | 'consent' = preview ? 'consent' : 'url';

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      data-cy={dataCy.dialogDataCy}
    >
      <DialogTitle>
        <T
          keyName="organization_apps_register_dialog_title"
          defaultValue="Register app"
        />
      </DialogTitle>

      {step === 'url' && (
        <form onSubmit={handlePreview}>
          <DialogContent>
            {description && <Box mb={1}>{description}</Box>}
            <TextField
              data-cy={dataCy.manifestUrlDataCy}
              label={t(
                'organization_apps_register_manifest_url_label',
                'Manifest URL'
              )}
              helperText={t(
                'organization_apps_register_manifest_url_helper',
                'URL to the app manifest.json file.'
              )}
              value={manifestUrl}
              onChange={(event) => setManifestUrl(event.target.value)}
              fullWidth
              autoFocus
              margin="normal"
              placeholder="https://my-plugin.example.com/manifest.json"
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>
              <T keyName="cancel_button" defaultValue="Cancel" />
            </Button>
            <LoadingButton
              data-cy={dataCy.continueDataCy}
              type="submit"
              variant="contained"
              color="primary"
              loading={previewLoading}
              disabled={!manifestUrl}
            >
              <T
                keyName="organization_apps_register_continue"
                defaultValue="Continue"
              />
            </LoadingButton>
          </DialogActions>
        </form>
      )}

      {step === 'consent' && preview && (
        <>
          <DialogContent data-cy={dataCy.consentDataCy}>
            <Box mb={2}>
              <AppSummary
                name={preview.name}
                version={preview.version}
                url={preview.baseUrl}
              />
            </Box>

            <Typography variant="body2" mb={1}>
              <T
                keyName="organization_apps_register_consent_intro"
                defaultValue="This app requests the following permissions:"
              />
            </Typography>

            {preview.requestedScopes.length === 0 && (
              <Typography
                variant="body2"
                color="text.secondary"
                data-cy={dataCy.noScopesDataCy}
              >
                <T
                  keyName="organization_apps_register_consent_no_scopes"
                  defaultValue="No scopes requested."
                />
              </Typography>
            )}

            <AppChips
              items={preview.requestedScopes}
              dataCy={dataCy.scopesDataCy}
            />
          </DialogContent>
          <DialogActions>
            <Button
              data-cy={dataCy.backDataCy}
              onClick={() => setPreview(null)}
              disabled={registerLoading}
            >
              <T
                keyName="organization_apps_register_back"
                defaultValue="Back"
              />
            </Button>
            <LoadingButton
              data-cy={dataCy.submitDataCy}
              variant="contained"
              color="primary"
              loading={registerLoading}
              onClick={handleRegister}
            >
              <T
                keyName="organization_apps_register_submit"
                defaultValue="Approve & install"
              />
            </LoadingButton>
          </DialogActions>
        </>
      )}
    </Dialog>
  );
};
