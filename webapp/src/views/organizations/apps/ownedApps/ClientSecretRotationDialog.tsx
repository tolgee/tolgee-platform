import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { AppCredentialsDisclosure } from 'tg.component/apps/AppCredentialsDisclosure';

type AppSecretRotationModel = components['schemas']['AppSecretRotationModel'];

type Props = {
  organizationId: number;
  appId: number;
  appName: string;
  onClose: () => void;
};

const OWNED_PREFIX = '/v2/organizations/{organizationId}/owned-apps';
const DAY = 86400;
const WEEK = 604800;

/**
 * Rolls the app's client secret: a new secret is minted and delivered to the app (so an SDK app
 * adopts it automatically), and the old one keeps working through a grace window the operator picks
 * — never cut off here, because a received delivery does not prove the app adopted the secret. The
 * old secret is revoked from the secrets list once the operator has confirmed the switch.
 */
export const ClientSecretRotationDialog = ({
  organizationId,
  appId,
  appName,
  onClose,
}: Props) => {
  const formatDate = useDateFormatter();
  const [graceSeconds, setGraceSeconds] = useState(DAY);
  const [result, setResult] = useState<AppSecretRotationModel | null>(null);
  const [capReached, setCapReached] = useState(false);

  const rollMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets/rotate',
    method: 'post',
    invalidatePrefix: OWNED_PREFIX,
  });

  const handleRoll = () =>
    rollMutation.mutate(
      {
        path: { organizationId, appId },
        content: { 'application/json': { graceSeconds } },
      },
      {
        onSuccess: (data) => setResult(data),
        onError: (error) => {
          if (error.code === 'app_too_many_live_secrets') {
            setCapReached(true);
            return;
          }
          error.handleError?.();
        },
      }
    );

  const delivered = result?.secret?.delivery?.delivered === true;

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="client-secret-rotation-dialog"
    >
      <DialogTitle>
        <T
          keyName="client_secret_rotation_title"
          defaultValue="Rotate client secret"
        />
      </DialogTitle>
      <DialogContent>
        {result ? (
          <>
            <Box mb={2}>
              <AppCredentialsDisclosure
                clientSecret={result.secret?.secret}
                delivery={result.secret?.delivery}
                dataCy="client-secret-rotation-new"
              />
            </Box>
            {result.previousExpiresAt &&
              (delivered ? (
                <Alert
                  severity="success"
                  data-cy="client-secret-rotation-outcome"
                >
                  <T
                    keyName="client_secret_rotation_outcome_delivered"
                    defaultValue="The new secret was delivered to {name}. The previous one keeps working until {date} — revoke it from the list once you have confirmed the app switched."
                    params={{
                      name: appName,
                      date: formatDate(result.previousExpiresAt, {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      }),
                    }}
                  />
                </Alert>
              ) : (
                <Alert severity="info" data-cy="client-secret-rotation-outcome">
                  <T
                    keyName="client_secret_rotation_outcome_grace"
                    defaultValue="Copy the new secret into your app. The previous secret keeps working until {date}, then stops on its own."
                    params={{
                      date: formatDate(result.previousExpiresAt, {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      }),
                    }}
                  />
                </Alert>
              ))}
          </>
        ) : (
          <>
            <Typography variant="body2" color="text.secondary" mb={2}>
              <T
                keyName="client_secret_rotation_intro"
                defaultValue="A new secret is created and delivered to {name} over the lifecycle channel. The old secret keeps working for the window below, then stops on its own — or revoke it from the list earlier, once you have confirmed the app switched."
                params={{ name: appName }}
              />
            </Typography>
            {capReached && (
              <Alert
                severity="warning"
                sx={{ mb: 2 }}
                data-cy="client-secret-rotation-cap-warning"
              >
                <T
                  keyName="client_secret_rotation_cap_reached"
                  defaultValue="The app already has the maximum number of active secrets. Revoke one from the list first, then rotate."
                />
              </Alert>
            )}
            <FormControl>
              <Typography variant="subtitle2" mb={0.5}>
                <T
                  keyName="client_secret_rotation_grace_label"
                  defaultValue="Keep the old secret working for"
                />
              </Typography>
              <RadioGroup
                value={String(graceSeconds)}
                onChange={(e) => setGraceSeconds(Number(e.target.value))}
              >
                <FormControlLabel
                  value={String(DAY)}
                  control={<Radio data-cy="client-secret-rotation-grace-day" />}
                  label={
                    <T
                      keyName="client_secret_rotation_grace_day"
                      defaultValue="24 hours"
                    />
                  }
                />
                <FormControlLabel
                  value={String(WEEK)}
                  control={
                    <Radio data-cy="client-secret-rotation-grace-week" />
                  }
                  label={
                    <T
                      keyName="client_secret_rotation_grace_week"
                      defaultValue="7 days"
                    />
                  }
                />
              </RadioGroup>
            </FormControl>
          </>
        )}
      </DialogContent>
      <DialogActions>
        {result ? (
          <Button
            data-cy="client-secret-rotation-close"
            variant="contained"
            color="primary"
            onClick={onClose}
          >
            <T keyName="global_close_button" defaultValue="Close" />
          </Button>
        ) : (
          <>
            <Button data-cy="client-secret-rotation-cancel" onClick={onClose}>
              <T keyName="global_cancel_button" defaultValue="Cancel" />
            </Button>
            <LoadingButton
              data-cy="client-secret-rotation-roll"
              variant="contained"
              color="primary"
              loading={rollMutation.isLoading}
              onClick={handleRoll}
            >
              <T
                keyName="client_secret_rotation_roll_button"
                defaultValue="Rotate now"
              />
            </LoadingButton>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};
