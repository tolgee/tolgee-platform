import { Tooltip, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';

type OwnedAppModel = components['schemas']['OwnedAppModel'];

type Props = {
  app: OwnedAppModel;
};

const PILL_COLORS = {
  light: {
    ok: { text: '#0c8a4c', background: '#e3f5ec' },
    err: { text: '#c62838', background: '#fdebed' },
  },
  dark: {
    ok: { text: '#5fce8f', background: '#17301f' },
    err: { text: '#ff8d99', background: '#38222a' },
  },
} as const;

const StyledPill = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  line-height: 1.5;
  cursor: default;
`;

const StyledDot = styled('span')`
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
  background: currentColor;
`;

/** Compact manifest-health pill with a last-checked / failure tooltip. */
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
      <StyledPill
        data-cy="owned-app-health-chip"
        data-cy-healthy={String(healthy)}
        sx={(theme) => {
          const colors =
            PILL_COLORS[theme.palette.mode][healthy ? 'ok' : 'err'];
          return { color: colors.text, background: colors.background };
        }}
      >
        <StyledDot />
        {healthy ? (
          <T keyName="owned_app_health_healthy" defaultValue="Healthy" />
        ) : (
          <T
            keyName="owned_app_health_not_healthy"
            defaultValue="Not healthy"
          />
        )}
      </StyledPill>
    </Tooltip>
  );
};
