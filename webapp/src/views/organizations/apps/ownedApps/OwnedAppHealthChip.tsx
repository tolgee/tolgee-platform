import { Chip, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';

type OwnedAppModel = components['schemas']['OwnedAppModel'];

type Props = {
  app: OwnedAppModel;
};

/** Compact manifest-health chip with a last-checked / failure tooltip. */
export const OwnedAppHealthChip = ({ app }: Props) => {
  const formatDate = useDateFormatter();
  const format = (value: number) =>
    formatDate(value, { dateStyle: 'short', timeStyle: 'short' });

  const unhealthy = Boolean(app.unhealthySince);
  const healthy = !unhealthy && app.manifestFailureCount === 0;

  const tooltip = healthy ? (
    app.manifestLastCheckedAt ? (
      <T
        keyName="owned_app_health_last_checked"
        defaultValue="Manifest last checked {date}"
        params={{ date: format(app.manifestLastCheckedAt) }}
      />
    ) : (
      <T
        keyName="owned_app_health_not_checked"
        defaultValue="Manifest not checked yet"
      />
    )
  ) : (
    <T
      keyName="owned_app_health_failing_tooltip"
      defaultValue="Last checked {date}. {error}"
      params={{
        date: app.manifestLastCheckedAt
          ? format(app.manifestLastCheckedAt)
          : '—',
        error: app.manifestLastError ?? '',
      }}
    />
  );

  return (
    <Tooltip title={tooltip}>
      <Chip
        size="small"
        color={healthy ? 'success' : 'error'}
        data-cy="owned-app-health-chip"
        data-cy-healthy={String(healthy)}
        label={
          healthy ? (
            <T keyName="owned_app_health_healthy" defaultValue="Healthy" />
          ) : (
            <T
              keyName="owned_app_health_not_healthy"
              defaultValue="Not healthy"
            />
          )
        }
      />
    </Tooltip>
  );
};
