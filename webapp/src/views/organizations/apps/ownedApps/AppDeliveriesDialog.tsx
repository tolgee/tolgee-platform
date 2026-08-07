import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';

type AppDeliveryModel = components['schemas']['AppDeliveryModel'];

type Props = {
  organizationId: number;
  appId: number;
  appName: string;
  onClose: () => void;
};

const StyledItem = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  padding: ${({ theme }) => theme.spacing(1.5, 0)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledHead = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  flex-wrap: wrap;
`;

type DeliveryState = 'delivered' | 'abandoned' | 'retrying';

const getState = (delivery: AppDeliveryModel): DeliveryState => {
  if (delivery.deliveredAt) return 'delivered';
  if (delivery.abandonedAt) return 'abandoned';
  return 'retrying';
};

const StateChip = ({ state }: { state: DeliveryState }) => {
  if (state === 'delivered') {
    return (
      <Chip
        size="small"
        color="success"
        data-cy="owned-app-deliveries-state"
        data-cy-state="delivered"
        label={
          <T
            keyName="owned_app_deliveries_state_delivered"
            defaultValue="Delivered"
          />
        }
      />
    );
  }
  if (state === 'abandoned') {
    return (
      <Chip
        size="small"
        color="error"
        data-cy="owned-app-deliveries-state"
        data-cy-state="abandoned"
        label={
          <T
            keyName="owned_app_deliveries_state_abandoned"
            defaultValue="Never delivered"
          />
        }
      />
    );
  }
  return (
    <Chip
      size="small"
      color="warning"
      data-cy="owned-app-deliveries-state"
      data-cy-state="retrying"
      label={
        <T
          keyName="owned_app_deliveries_state_retrying"
          defaultValue="Retrying"
        />
      }
    />
  );
};

/**
 * Every signed POST Tolgee made to the app's base URL. An abandoned delivery is how an owner
 * finds out that an install's credentials never reached the app.
 */
export const AppDeliveriesDialog = ({
  organizationId,
  appId,
  appName,
  onClose,
}: Props) => {
  const formatDate = useDateFormatter();
  const format = (value: number) =>
    formatDate(value, { dateStyle: 'short', timeStyle: 'short' });

  const deliveriesLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/deliveries',
    method: 'get',
    path: { organizationId, appId },
  });

  const deliveries = deliveriesLoadable.data?._embedded?.appDeliveries ?? [];

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="owned-app-deliveries-dialog"
    >
      <DialogTitle>
        <T
          keyName="owned_app_deliveries_dialog_title"
          defaultValue="Lifecycle deliveries of {name}"
          params={{ name: appName }}
        />
      </DialogTitle>

      <DialogContent>
        <Typography variant="body2" color="text.secondary" mb={1}>
          <T
            keyName="owned_app_deliveries_description"
            defaultValue="Signed events Tolgee sent to the app's base URL — registration, installs, uninstalls and rotated credentials."
          />
        </Typography>

        {deliveriesLoadable.isLoading && (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress size={24} />
          </Box>
        )}

        {!deliveriesLoadable.isLoading && deliveries.length === 0 && (
          <Typography
            variant="body2"
            color="text.secondary"
            data-cy="owned-app-deliveries-empty"
          >
            <T
              keyName="owned_app_deliveries_empty"
              defaultValue="Nothing has been delivered to this app yet."
            />
          </Typography>
        )}

        {deliveries.map((delivery) => {
          const state = getState(delivery);
          return (
            <StyledItem
              key={delivery.id}
              data-cy="owned-app-deliveries-item"
              data-cy-event-type={delivery.eventType}
            >
              <StyledHead>
                <Typography variant="subtitle2">
                  {delivery.eventType}
                </Typography>
                <StateChip state={state} />
                <Typography variant="caption" color="text.secondary">
                  <T
                    keyName="owned_app_deliveries_attempts"
                    defaultValue="{count, plural, one {# attempt} other {# attempts}}"
                    params={{ count: delivery.attempts }}
                  />
                </Typography>
              </StyledHead>

              <Typography variant="caption" color="text.secondary" noWrap>
                {delivery.targetUrl}
              </Typography>

              <Typography variant="caption" color="text.secondary">
                <T
                  keyName="owned_app_deliveries_created_at"
                  defaultValue="Triggered {date}"
                  params={{ date: format(delivery.createdAt) }}
                />
              </Typography>

              {state === 'delivered' && (
                <Typography variant="caption" color="text.secondary">
                  <T
                    keyName="owned_app_deliveries_delivered_at"
                    defaultValue="Delivered {date}"
                    params={{ date: format(delivery.deliveredAt!) }}
                  />
                </Typography>
              )}

              {state === 'abandoned' && (
                <Typography variant="caption" color="error">
                  <T
                    keyName="owned_app_deliveries_abandoned_explanation"
                    defaultValue="Tolgee stopped retrying {date}. The app never received this event — issue a new secret to have the credentials delivered again."
                    params={{ date: format(delivery.abandonedAt!) }}
                  />
                </Typography>
              )}

              {state === 'retrying' && delivery.lastAttemptAt && (
                <Typography variant="caption" color="text.secondary">
                  <T
                    keyName="owned_app_deliveries_last_attempt"
                    defaultValue="Last tried {date}, Tolgee keeps retrying."
                    params={{ date: format(delivery.lastAttemptAt) }}
                  />
                </Typography>
              )}

              {delivery.lastError && (
                <Typography variant="caption" color="text.secondary">
                  <T
                    keyName="owned_app_deliveries_last_error"
                    defaultValue="Last error: {error}"
                    params={{ error: delivery.lastError }}
                  />
                </Typography>
              )}
            </StyledItem>
          );
        })}
      </DialogContent>

      <DialogActions>
        <Button data-cy="owned-app-deliveries-close" onClick={onClose}>
          <T keyName="global_close_button" defaultValue="Close" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
