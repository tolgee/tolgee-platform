import { Box, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

type Props = {
  session: components['schemas']['UserSessionModel'];
};

/**
 * Location is only present when the instance is configured with a GeoIP database, so the IP stays
 * the fallback rather than the primary display.
 */
export function SessionLocation({ session }: Props) {
  const { city, country, ip } = session;

  const location =
    city && country ? (
      <T
        keyName="session-location"
        defaultValue="{city}, {country}"
        params={{ city, country }}
      />
    ) : (
      country || city
    );

  if (!location) {
    return (
      <Box data-cy="session-list-item-location">
        {ip || <T keyName="session-item-unknown-location" defaultValue="—" />}
      </Box>
    );
  }

  return (
    <Tooltip title={ip ?? ''} disableHoverListener={!ip}>
      <Box data-cy="session-list-item-location" sx={{ width: 'fit-content' }}>
        {location}
      </Box>
    </Tooltip>
  );
}
