import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { StyledBillingHint } from 'tg.component/billing/Decorations';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

type CommunityProfile = components['schemas']['CommunityProfileModel'];

const StyledTilesContainer = styled('div')`
  container-type: inline-size;
`;

const StyledTiles = styled(Box)`
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;

  @container (max-width: 560px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const StyledTile = styled(Box)`
  background-color: ${({ theme }) => theme.palette.tile.background};
  border-radius: 20px;
  height: 120px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  text-align: center;
  color: ${({ theme }) => theme.palette.text.primary};

  @container (max-width: 800px) {
    height: 100px;
  }
`;

const StyledValue = styled('div')`
  font-size: 28px;
  white-space: nowrap;

  @container (max-width: 800px) {
    font-size: 24px;
  }
`;

const StyledLabel = styled('div')`
  font-size: 18px;
  max-width: 100%;
  padding: 0 8px;
  box-sizing: border-box;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  @container (max-width: 800px) {
    font-size: 14px;
  }
`;

const TwoLineHint = ({ main, extra }: { main: string; extra: string }) => (
  <Box display="grid" gap={1}>
    <span>{main}</span>
    <span>{extra}</span>
  </Box>
);

type Props = {
  profile: CommunityProfile;
};

export const ProfileTotals = ({ profile }: Props) => {
  const { t } = useTranslate();
  const formatNumber = useNumberFormatter();
  const rate = profile.suggested
    ? Math.min(1, profile.accepted / profile.suggested)
    : 0;

  return (
    <StyledTilesContainer>
      <StyledTiles data-cy="community-profile-totals">
        <StyledTile>
          <StyledValue>{formatNumber(profile.suggested)}</StyledValue>
          <StyledLabel>
            <Tooltip
              disableInteractive
              title={
                <TwoLineHint
                  main={t(
                    'community_profile_suggestions_hint',
                    'All suggestions from all projects'
                  )}
                  extra={t(
                    'community_profile_suggestions_hint_extra',
                    'Accepted, declined and waiting for review.'
                  )}
                />
              }
            >
              <StyledBillingHint>
                {t('community_profile_suggestions', 'Suggestions')}
              </StyledBillingHint>
            </Tooltip>
          </StyledLabel>
        </StyledTile>
        <StyledTile>
          <StyledValue>{formatNumber(profile.accepted)}</StyledValue>
          <StyledLabel>
            {t('community_profile_accepted', 'Accepted')}
          </StyledLabel>
        </StyledTile>
        <StyledTile>
          <StyledValue>
            {formatNumber(rate, { style: 'percent', maximumFractionDigits: 0 })}
          </StyledValue>
          <StyledLabel>
            <Tooltip
              disableInteractive
              title={
                <TwoLineHint
                  main={t(
                    'community_profile_acceptance_rate_hint',
                    '{accepted} of {suggested, plural, one {# suggestion} other {# suggestions}} accepted',
                    { accepted: profile.accepted, suggested: profile.suggested }
                  )}
                  extra={t(
                    'community_profile_acceptance_rate_hint_extra',
                    'Waiting suggestions count too, so it also depends on review speed.'
                  )}
                />
              }
            >
              <StyledBillingHint>
                {t('community_profile_acceptance_rate', 'Acceptance rate')}
              </StyledBillingHint>
            </Tooltip>
          </StyledLabel>
        </StyledTile>
        <StyledTile>
          <StyledValue>{formatNumber(profile.contributions)}</StyledValue>
          <StyledLabel>
            <Tooltip
              disableInteractive
              title={
                <TwoLineHint
                  main={t(
                    'community_profile_contributions_hint',
                    'All changes in public projects'
                  )}
                  extra={t(
                    'community_profile_contributions_hint_extra',
                    'Translations, suggestions and comments, per language.'
                  )}
                />
              }
            >
              <StyledBillingHint>
                {t('community_profile_contributions', 'Contributions')}
              </StyledBillingHint>
            </Tooltip>
          </StyledLabel>
        </StyledTile>
      </StyledTiles>
    </StyledTilesContainer>
  );
};
