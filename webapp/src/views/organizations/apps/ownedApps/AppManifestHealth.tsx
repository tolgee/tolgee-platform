import { Box, Chip, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';

type OwnedAppModel = components['schemas']['OwnedAppModel'];

type Props = {
  app: OwnedAppModel;
};

const FailureKindLabel = ({ kind }: { kind?: string }) => {
  if (kind === 'UNREACHABLE') {
    return (
      <T
        keyName="owned_apps_manifest_unreachable"
        defaultValue="Manifest unreachable"
      />
    );
  }
  if (kind === 'INVALID') {
    return (
      <T
        keyName="owned_apps_manifest_invalid"
        defaultValue="Manifest invalid"
      />
    );
  }
  return (
    <T
      keyName="owned_apps_manifest_failing"
      defaultValue="Manifest check failing"
    />
  );
};

/**
 * Whether Tolgee can still read the app's manifest. An unhealthy app keeps working, but
 * it is on its way to being removed if the manifest never answers again.
 */
export const AppManifestHealth = ({ app }: Props) => {
  const formatDate = useDateFormatter();
  const format = (value: number) =>
    formatDate(value, { dateStyle: 'short', timeStyle: 'short' });

  const failing = app.manifestFailureCount > 0;
  const unhealthy = Boolean(app.unhealthySince);

  const getChipColor = () => {
    if (unhealthy) return 'error' as const;
    if (failing) return 'warning' as const;
    return 'success' as const;
  };

  return (
    <Box
      display="grid"
      gap={0.5}
      data-cy="owned-apps-item-health"
      data-cy-healthy={String(!failing && !unhealthy)}
    >
      <Box>
        <Chip
          size="small"
          color={getChipColor()}
          label={
            failing || unhealthy ? (
              <FailureKindLabel kind={app.manifestLastFailureKind} />
            ) : (
              <T
                keyName="owned_apps_manifest_healthy"
                defaultValue="Manifest healthy"
              />
            )
          }
        />
      </Box>

      {unhealthy && (
        <Typography variant="caption" color="error">
          <T
            keyName="owned_apps_manifest_unhealthy_since"
            defaultValue="Unhealthy since {date}. If the manifest keeps failing, Tolgee removes the app from every organization."
            params={{ date: format(app.unhealthySince!) }}
          />
        </Typography>
      )}

      {failing && (
        <Typography variant="caption" color="text.secondary">
          <T
            keyName="owned_apps_manifest_failure_count"
            defaultValue="{count, plural, one {# failed check in a row} other {# failed checks in a row}}, first failed {date}"
            params={{
              count: app.manifestFailureCount,
              date: app.manifestFirstFailedAt
                ? format(app.manifestFirstFailedAt)
                : '',
            }}
          />
        </Typography>
      )}

      {failing && app.manifestLastError && (
        <Typography variant="caption" color="text.secondary">
          <T
            keyName="owned_apps_manifest_last_error"
            defaultValue="Last error: {error}"
            params={{ error: app.manifestLastError }}
          />
        </Typography>
      )}

      {!failing && !unhealthy && app.manifestLastCheckedAt && (
        <Typography variant="caption" color="text.secondary">
          <T
            keyName="owned_apps_manifest_last_checked"
            defaultValue="Last checked {date}"
            params={{ date: format(app.manifestLastCheckedAt) }}
          />
        </Typography>
      )}
    </Box>
  );
};
