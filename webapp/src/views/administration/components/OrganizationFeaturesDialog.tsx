import { ReactNode, useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  styled,
  Typography,
} from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { Feature } from 'tg.service/apiSchemaTypes';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useIsAdmin, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useFeatures } from 'tg.translationTools/useFeatures';

const StyledSection = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};
`;

const StyledItems = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr));
`;

const StyledLabel = styled('span')`
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5, 1)};
`;

type Props = {
  organizationId: number;
  organizationName: string;
  onClose: () => void;
};

function setIncluded<V>(list: V[], value: V, included: boolean) {
  const without = list.filter((item) => item !== value);
  return included ? [...without, value] : without;
}

const FeatureSection = ({
  title,
  hint,
  children,
}: {
  title: ReactNode;
  hint: ReactNode;
  children: ReactNode;
}) => (
  <StyledSection>
    <Typography variant="h6">{title}</Typography>
    <Typography variant="body2" color="text.secondary">
      {hint}
    </Typography>
    <StyledItems>{children}</StyledItems>
  </StyledSection>
);

export const OrganizationFeaturesDialog = ({
  organizationId,
  organizationName,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  const message = useMessage();
  const isAdmin = useIsAdmin();
  const featureLabels = useFeatures();
  const { preferredOrganization } = usePreferredOrganization();
  const { refetchInitialData } = useGlobalActions();

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
      setGrantedFeatures(
        loadable.data.planFeatures
          .filter((f) => f.granted)
          .map((f) => f.feature)
      );
    }
  }, [loadable.data]);

  const planFeatureGrantsAllowed = Boolean(
    loadable.data?.planFeatureGrantsAllowed
  );

  const planFeatures = loadable.data?.planFeatures ?? [];
  const visibleFeatures = planFeatures.filter((item) => !item.hidden);
  const hiddenFeatures = planFeatures.filter((item) => item.hidden);

  const handleToggle = (feature: Feature) => (_: unknown, checked: boolean) =>
    setGrantedFeatures((prev) => setIncluded(prev, feature, checked));

  function handleSave() {
    saveMutation.mutate(
      {
        path: { organizationId },
        content: {
          'application/json': {
            grantedFeatures,
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
        ) : !planFeatureGrantsAllowed ? (
          <T
            keyName="administration_organization_plan_feature_grants_not_allowed"
            defaultValue="Plan features cannot be granted on this server. Self-hosted instances get plan features only from their license."
          />
        ) : (
          <>
            <FeatureSection
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
            >
              {visibleFeatures.map((item) => (
                <FormControlLabel
                  key={item.feature}
                  disabled={!isAdmin || item.fromPlan}
                  control={
                    <Checkbox
                      data-cy="administration-organization-plan-feature-checkbox"
                      data-cy-feature={item.feature}
                      checked={
                        item.fromPlan || grantedFeatures.includes(item.feature)
                      }
                      onChange={handleToggle(item.feature)}
                    />
                  }
                  label={
                    <StyledLabel>
                      {featureLabels[item.feature] ?? item.feature}
                      {item.fromPlan && (
                        <Chip
                          size="small"
                          color="success"
                          data-cy="administration-organization-plan-feature-from-plan"
                          label={t(
                            'administration_organization_plan_feature_from_plan',
                            'From plan'
                          )}
                        />
                      )}
                      {item.fromPlan && item.granted && (
                        <Chip
                          size="small"
                          data-cy="administration-organization-plan-feature-granted"
                          label={t(
                            'administration_organization_plan_feature_granted',
                            'Granted'
                          )}
                        />
                      )}
                    </StyledLabel>
                  }
                />
              ))}
            </FeatureSection>
            {hiddenFeatures.length > 0 && (
              <FeatureSection
                title={
                  <T
                    keyName="administration_organization_hidden_features_title"
                    defaultValue="Hidden features"
                  />
                }
                hint={
                  <T
                    keyName="administration_organization_hidden_features_hint"
                    defaultValue="Features in development. Users cannot see or use them until granted here."
                  />
                }
              >
                {hiddenFeatures.map((item) => (
                  <FormControlLabel
                    key={item.feature}
                    disabled={!isAdmin}
                    control={
                      <Checkbox
                        data-cy="administration-organization-hidden-feature-checkbox"
                        data-cy-feature={item.feature}
                        checked={grantedFeatures.includes(item.feature)}
                        onChange={handleToggle(item.feature)}
                      />
                    }
                    label={
                      <StyledLabel>
                        {featureLabels[item.feature] ?? item.feature}
                      </StyledLabel>
                    }
                  />
                ))}
              </FeatureSection>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        {isAdmin && planFeatureGrantsAllowed ? (
          <Box display="flex" gap={1}>
            <Button onClick={onClose}>
              <T keyName="global_cancel_button" />
            </Button>
            <LoadingButton
              data-cy="administration-organization-features-save"
              variant="contained"
              color="primary"
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
