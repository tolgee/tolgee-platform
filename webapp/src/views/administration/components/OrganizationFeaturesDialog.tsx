import { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Feature, FeatureFlag } from 'tg.service/apiSchemaTypes';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useFeatures } from 'tg.translationTools/useFeatures';
import { useFeatureFlagLabels } from 'tg.translationTools/useFeatureFlagLabels';
import { FeatureCheckboxSection } from 'tg.views/administration/components/FeatureCheckboxSection';

type Props = {
  organizationId: number;
  organizationName: string;
  onClose: () => void;
};

export const OrganizationFeaturesDialog = ({
  organizationId,
  organizationName,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  const message = useMessage();
  const isAdmin = useIsAdmin();
  const featureLabels = useFeatures();
  const featureFlagLabels = useFeatureFlagLabels();
  const { preferredOrganization } = usePreferredOrganization();
  const { refetchInitialData } = useGlobalActions();

  const [featureFlags, setFeatureFlags] = useState<FeatureFlag[]>([]);
  const [grantedFeatures, setGrantedFeatures] = useState<Feature[]>([]);

  const loadable = useApiQuery({
    url: '/v2/administration/organizations/{organizationId}/features',
    method: 'get',
    path: { organizationId },
  });

  const saveMutation = useApiMutation({
    url: '/v2/administration/organizations/{organizationId}/features',
    method: 'put',
    invalidatePrefix: '/v2/administration',
  });

  useEffect(() => {
    if (loadable.data) {
      setFeatureFlags(
        loadable.data.featureFlags.filter((f) => f.granted).map((f) => f.flag)
      );
      setGrantedFeatures(
        loadable.data.planFeatures
          .filter((f) => f.granted)
          .map((f) => f.feature)
      );
    }
  }, [loadable.data]);

  function toggle<V>(list: V[], value: V, checked: boolean) {
    const without = list.filter((item) => item !== value);
    return checked ? [...without, value] : without;
  }

  const planFeatureGrantsAllowed = Boolean(
    loadable.data?.planFeatureGrantsAllowed
  );

  function handleSave() {
    saveMutation.mutate(
      {
        path: { organizationId },
        content: {
          'application/json': {
            featureFlags,
            grantedFeatures: planFeatureGrantsAllowed ? grantedFeatures : [],
          },
        },
      },
      {
        onSuccess() {
          message.success(
            <T
              keyName="administration_organization_features_saved"
              defaultValue="Organization features saved"
            />
          );
          if (preferredOrganization?.id === organizationId) {
            refetchInitialData();
          }
          onClose();
        },
      }
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-cy="administration-organization-features-dialog"
    >
      <DialogTitle>
        <T
          keyName="administration_organization_features_title"
          defaultValue="Features of {organizationName}"
          params={{ organizationName }}
        />
      </DialogTitle>
      <DialogContent>
        {!loadable.data ? (
          <BoxLoading />
        ) : (
          <>
            <FeatureCheckboxSection
              title={
                <T
                  keyName="administration_organization_feature_flags_title"
                  defaultValue="Feature flags"
                />
              }
              hint={
                <T
                  keyName="administration_organization_feature_flags_hint"
                  defaultValue="Feature flags reveal features that are not generally available yet. Without the flag, the feature is completely hidden."
                />
              }
              layout="list"
              checkboxDataCy="administration-organization-feature-flag-checkbox"
              items={loadable.data.featureFlags.map((item) => ({
                key: item.flag,
                label: featureFlagLabels[item.flag],
                chip: item.enabledForAll && (
                  <Chip
                    size="small"
                    label={t(
                      'administration_organization_feature_flag_enabled_for_all',
                      'Enabled for all organizations'
                    )}
                  />
                ),
                checked: item.enabledForAll || featureFlags.includes(item.flag),
                disabled: !isAdmin || item.enabledForAll,
                onChange: (checked) =>
                  setFeatureFlags(toggle(featureFlags, item.flag, checked)),
                checkboxAttributes: { 'data-cy-flag': item.flag },
              }))}
            />
            {planFeatureGrantsAllowed && (
              <FeatureCheckboxSection
                title={
                  <T
                    keyName="administration_organization_plan_features_title"
                    defaultValue="Granted plan features"
                  />
                }
                hint={
                  <T
                    keyName="administration_organization_plan_features_hint"
                    defaultValue="Granted features are available to the organization regardless of its plan."
                  />
                }
                layout="grid"
                checkboxDataCy="administration-organization-plan-feature-checkbox"
                items={loadable.data.planFeatures.map((item) => ({
                  key: item.feature,
                  label: featureLabels[item.feature],
                  chip: item.fromPlan && (
                    <Chip
                      size="small"
                      color="success"
                      data-cy="administration-organization-plan-feature-from-plan"
                      label={t(
                        'administration_organization_plan_feature_from_plan',
                        'From plan'
                      )}
                    />
                  ),
                  checked: grantedFeatures.includes(item.feature),
                  disabled: !isAdmin,
                  onChange: (checked) =>
                    setGrantedFeatures(
                      toggle(grantedFeatures, item.feature, checked)
                    ),
                  checkboxAttributes: { 'data-cy-feature': item.feature },
                }))}
              />
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        {isAdmin ? (
          <Box display="flex" gap={1}>
            <Button onClick={onClose}>
              <T keyName="global_cancel_button" />
            </Button>
            <LoadingButton
              data-cy="administration-organization-features-save"
              variant="contained"
              color="primary"
              disabled={!loadable.data}
              loading={saveMutation.isLoading}
              onClick={handleSave}
            >
              <T keyName="global_form_save" />
            </LoadingButton>
          </Box>
        ) : (
          <Button onClick={onClose}>
            <T keyName="global_close_button" />
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
