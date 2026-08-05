import { Box, styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { components } from 'tg.service/apiSchema.generated';

import { countryCodeToFlagEmoji } from './countryCodeToFlagEmoji';
import { isLocalIp } from './isLocalIp';

const StyledRoot = styled(Box)`
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
`;

const StyledFlag = styled(FlagImage)`
  width: 18px;
  flex-shrink: 0;
`;

const StyledLabel = styled(Box)`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  session: components['schemas']['UserSessionModel'];
};

/**
 * Location is only present when the instance is configured with a GeoIP database, so the IP stays
 * the fallback rather than the primary display. The flag carries the country, which keeps long
 * country names out of a column that has to stay narrow - the tooltip spells it out.
 */
export function SessionLocation({ session }: Props) {
  const { city, country, countryCode, ip } = session;

  const flagEmoji = countryCode
    ? countryCodeToFlagEmoji(countryCode)
    : undefined;

  const label = city || country;

  if (!label) {
    const fallback = !ip ? (
      <T keyName="session-item-unknown-location" defaultValue="—" />
    ) : isLocalIp(ip) ? (
      <T keyName="session-location-local" defaultValue="Local network" />
    ) : (
      ip
    );

    return (
      <Tooltip title={ip ?? ''} disableHoverListener={!ip}>
        <StyledRoot
          data-cy="session-list-item-location"
          data-cy-location-kind={ip && isLocalIp(ip) ? 'local' : 'ip'}
        >
          <StyledLabel>{fallback}</StyledLabel>
        </StyledRoot>
      </Tooltip>
    );
  }

  const full =
    city && country ? (
      <T
        keyName="session-location"
        defaultValue="{city}, {country}"
        params={{ city, country }}
      />
    ) : (
      label
    );

  return (
    <Tooltip
      title={
        <Box>
          <Box>{full}</Box>
          {ip && <Box>{ip}</Box>}
        </Box>
      }
    >
      <StyledRoot
        data-cy="session-list-item-location"
        data-cy-location-kind="resolved"
      >
        {flagEmoji && <StyledFlag flagEmoji={flagEmoji} />}
        <StyledLabel>{label}</StyledLabel>
      </StyledRoot>
    </Tooltip>
  );
}
