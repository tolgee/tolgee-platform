import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  AppRegisterDialog,
  AppRegisterDialogDataCy,
} from 'tg.component/apps/AppRegisterDialog';

const DATA_CY: AppRegisterDialogDataCy = {
  dialogDataCy: 'organization-apps-register-dialog',
  manifestUrlDataCy: 'organization-apps-register-manifest-url',
  continueDataCy: 'organization-apps-register-continue',
  consentDataCy: 'organization-apps-register-consent',
  noScopesDataCy: 'organization-apps-register-consent-no-scopes',
  scopesDataCy: 'organization-apps-register-consent-scope',
  backDataCy: 'organization-apps-register-back',
  submitDataCy: 'organization-apps-register-submit',
};

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterAppDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  const handleClose = () => {
    previewMutation.reset();
    registerMutation.reset();
    onClose();
  };

  return (
    <AppRegisterDialog
      open={open}
      onClose={handleClose}
      previewLoading={previewMutation.isLoading}
      registerLoading={registerMutation.isLoading}
      onPreview={(manifestUrl, onSuccess) => {
        if (!organization) return;
        previewMutation.mutate(
          {
            path: { organizationId: organization.id },
            content: { 'application/json': { manifestUrl } },
          },
          { onSuccess }
        );
      }}
      onRegister={(manifestUrl, onSuccess) => {
        if (!organization) return;
        registerMutation.mutate(
          {
            path: { organizationId: organization.id },
            content: { 'application/json': { manifestUrl } },
          },
          { onSuccess: () => onSuccess() }
        );
      }}
      dataCy={DATA_CY}
    />
  );
};
