import {
  Box,
  Chip,
  Tooltip,
  Typography,
  styled,
  useTheme,
} from '@mui/material';
import { Fragment, useEffect, useState } from 'react';
import clsx from 'clsx';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { StyledCircledPill } from 'tg.component/languages/CircledLanguageIconList';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { UserName } from 'tg.component/common/UserName';
import { Calendar, InfoCircle } from '@untitled-ui/icons-react';
import { StatesBarContainer } from 'tg.views/projects/TranslationStatesBar';
import { PercentFormat } from 'tg.views/projects/dashboard/PercentFormat';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { useDateFormatter } from 'tg.hooks/useLocale';

type LanguageModel = components['schemas']['LanguageModel'];

const MAX_FLAGS = 5;
const BAR_HEIGHT = 8;
const TOOLTIP_DELAY = 300;
const delayedTooltip = {
  enterDelay: TOOLTIP_DELAY,
  enterNextDelay: TOOLTIP_DELAY,
  disableInteractive: true,
};

const StyledCard = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  border-radius: 10px;
  padding: ${({ theme }) => theme.spacing(2.5)};
  display: grid;
  gap: ${({ theme }) => theme.spacing(1.5)};
  align-content: start;
  color: ${({ theme }) => theme.palette.text.primary};
  &.me {
    background: ${({ theme }) => theme.palette.tile.background};
    border-color: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.tokens.secondary.main
        : theme.palette.tokens.border.secondary};
  }
`;

export type ContributorCardProps = {
  id: number;
  name?: string;
  avatar?: components['schemas']['Avatar'];
  languages?: LanguageModel[];
  contributions?: number;
  suggested?: number;
  accepted?: number;
  declined?: number;
  firstContributionAt?: number;
  lastContributionAt?: number;
  isMe: boolean;
};

export const ContributorCard = ({
  languages = [],
  ...props
}: ContributorCardProps) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const timeDistance = useTimeDistance();
  const formatDate = useDateFormatter();
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setLoaded(true), 50);
    return () => clearTimeout(timer);
  }, []);

  const shownLanguages = languages.slice(0, MAX_FLAGS);
  const hiddenLanguages = languages.slice(MAX_FLAGS);
  const hasStats = props.suggested !== undefined;
  const suggested = props.suggested ?? 0;
  const accepted = props.accepted ?? 0;
  const declined = props.declined ?? 0;
  const hasSuggestions = suggested > 0;

  const acceptedColor = theme.palette.tokens.state.reviewed;
  const acceptedPercent = suggested
    ? Math.min(accepted / suggested, 1) * 100
    : 0;
  const outcomes = [
    {
      key: 'accepted',
      color: acceptedColor,
      label: t('community_contributor_outcome_accepted', 'Accepted'),
      count: accepted,
    },
    {
      key: 'declined',
      color: theme.palette.tokens.state.untranslated,
      label: t('community_contributor_outcome_declined', 'Declined'),
      count: declined,
    },
  ].filter((o) => o.count > 0);

  const outcomeLabels = (
    <Box
      display="grid"
      gridTemplateColumns="auto auto auto auto"
      alignItems="center"
      columnGap={1}
      rowGap={0.625}
      m={1}
      whiteSpace="nowrap"
      data-cy="community-contributor-card-outcomes"
    >
      {outcomes.map((o) => (
        <Fragment key={o.key}>
          <Box width={8} height={8} borderRadius="50%" bgcolor={o.color} />
          <Box>{o.label}</Box>
          <Box ml={1}>
            {o.key === 'accepted' && <PercentFormat number={acceptedPercent} />}
          </Box>
          <Box ml={1}>
            {t(
              'community_contributor_outcome_count',
              '{count, plural, one {# suggestion} other {# suggestions}}',
              { count: o.count }
            )}
          </Box>
        </Fragment>
      ))}
    </Box>
  );

  const activityLabels = (
    <Box
      display="grid"
      gridTemplateColumns="auto auto"
      columnGap={2}
      rowGap={0.625}
      m={1}
      whiteSpace="nowrap"
      data-cy="community-contributor-card-activity"
    >
      {props.lastContributionAt && (
        <>
          <Box>{t('community_contributor_activity_last', 'Last active')}</Box>
          <Box data-cy="community-contributor-card-last-active-date">
            {formatDate(new Date(props.lastContributionAt), {
              dateStyle: 'medium',
            })}
          </Box>
        </>
      )}
      {props.firstContributionAt && (
        <>
          <Box>
            {t('community_contributor_activity_since', 'Contributing since')}
          </Box>
          <Box data-cy="community-contributor-card-since-date">
            {formatDate(new Date(props.firstContributionAt), {
              dateStyle: 'medium',
            })}
          </Box>
        </>
      )}
    </Box>
  );

  const header = (
    <Box
      display="grid"
      gridTemplateColumns="40px minmax(0, 1fr) auto"
      gridTemplateRows="24px 16px"
      columnGap={1.5}
      alignItems="center"
    >
      <Box gridRow="1 / span 2" display="flex">
        <AvatarImg
          owner={{
            name: props.name,
            avatar: props.avatar,
            type: 'USER',
            id: props.id,
          }}
          size={40}
        />
      </Box>
      <Box display="flex" alignItems="center" gap={1} minWidth={0}>
        <Typography variant="body2" fontWeight={500} noWrap component="span">
          <UserName name={props.name} />
        </Typography>
        {props.isMe && (
          <Chip size="small" label={t('community_contributor_you', 'You')} />
        )}
      </Box>
      <Box gridColumn={2} minWidth={0}>
        <Box display="flex" alignItems="center" gap={0.5} height={16}>
          {shownLanguages.map((l) => (
            <Tooltip key={l.id} title={l.name} {...delayedTooltip}>
              <Box display="flex" data-cy="community-contributor-card-flag">
                <FlagImage flagEmoji={l.flagEmoji || ''} height={16} />
              </Box>
            </Tooltip>
          ))}
          {hiddenLanguages.length > 0 && (
            <Tooltip
              title={hiddenLanguages.map((l) => l.name).join(', ')}
              {...delayedTooltip}
            >
              <div>
                <StyledCircledPill
                  height={16}
                  width={36}
                  wrapperProps={{ className: 'wrapped' }}
                >
                  +{hiddenLanguages.length}
                </StyledCircledPill>
              </div>
            </Tooltip>
          )}
        </Box>
      </Box>
    </Box>
  );

  const suggestions = hasStats && (
    <Tooltip
      title={outcomes.length ? outcomeLabels : ''}
      componentsProps={{ tooltip: { style: { maxWidth: '100vw' } } }}
      {...delayedTooltip}
    >
      <Box display="grid" gap={1}>
        <Box
          display="flex"
          justifyContent="space-between"
          alignItems="center"
          gap={1}
        >
          <Typography
            variant="body2"
            fontWeight={hasSuggestions ? 500 : undefined}
            color={hasSuggestions ? undefined : 'text.secondary'}
            whiteSpace="nowrap"
            data-cy="community-contributor-card-suggestions"
          >
            {hasSuggestions
              ? t(
                  'community_contributor_suggestions',
                  '{count, plural, one {{count, number, ::compact-short} suggestion} other {{count, number, ::compact-short} suggestions}}',
                  { count: suggested }
                )
              : t('community_contributor_no_suggestions', 'No suggestions yet')}
          </Typography>
          {hasSuggestions && (
            <Box
              display="flex"
              alignItems="center"
              gap={0.75}
              fontSize={13}
              color="text.secondary"
              whiteSpace="nowrap"
              data-cy="community-contributor-card-accepted"
            >
              <Box
                width={8}
                height={8}
                borderRadius="50%"
                bgcolor={acceptedColor}
              />
              {t(
                'community_contributor_accepted',
                '{count, plural, one {{count, number, ::compact-short} accepted} other {{count, number, ::compact-short} accepted}}',
                { count: accepted }
              )}
            </Box>
          )}
        </Box>
        <StatesBarContainer data-cy="community-contributor-bar">
          <div className="bar">
            {accepted > 0 && (
              <Box
                className={clsx({ state: true, loadedState: !loaded })}
                style={{
                  width: `calc(max(${acceptedPercent}%, 8px) + ${
                    BAR_HEIGHT / 2
                  }px)`,
                  backgroundColor: acceptedColor,
                }}
              />
            )}
          </div>
        </StatesBarContainer>
      </Box>
    </Tooltip>
  );

  const footer = (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems="center"
      gap={1}
      fontSize={13}
      color="text.secondary"
      whiteSpace="nowrap"
    >
      {props.contributions !== undefined ? (
        <Tooltip
          title={t(
            'community_contributor_contributions_hint',
            'translation, review, comment or suggestion'
          )}
          {...delayedTooltip}
        >
          <Box
            display="flex"
            alignItems="center"
            gap={0.5}
            data-cy="community-contributor-card-contributions"
          >
            {t(
              'community_contributor_contributions',
              '{count, plural, one {{count, number, ::compact-short} contribution} other {{count, number, ::compact-short} contributions}}',
              { count: props.contributions }
            )}
            <InfoCircle width={14} height={14} />
          </Box>
        </Tooltip>
      ) : (
        <span />
      )}
      {props.lastContributionAt && (
        <Tooltip title={activityLabels} {...delayedTooltip}>
          <Box
            display="flex"
            alignItems="center"
            gap={0.5}
            data-cy="community-contributor-card-last-active"
          >
            {t('community_contributor_last_active', 'Active {time}', {
              time: timeDistance(props.lastContributionAt),
            })}
            <Calendar width={14} height={14} />
          </Box>
        </Tooltip>
      )}
    </Box>
  );

  const content = (
    <>
      {header}
      {suggestions}
      {hasStats && footer}
    </>
  );

  return (
    <StyledCard
      className={props.isMe ? 'me' : undefined}
      data-cy="community-contributor-card"
    >
      {content}
    </StyledCard>
  );
};
