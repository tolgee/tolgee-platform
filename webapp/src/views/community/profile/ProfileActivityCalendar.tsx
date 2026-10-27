import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import {
  alpha,
  Box,
  darken,
  lighten,
  MenuItem,
  Select,
  styled,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import {
  CommunityChartMode,
  useCommunityModeTotalLabel,
} from 'tg.views/projects/community/contributors/CommunityActivityChart';
import { getDayOfYear, getDaysInYear, parseISO } from 'date-fns';
import { ActivityCalendar, Activity } from 'react-activity-calendar';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useWidthObserver } from 'tg.hooks/useWidthObserver';
import { components } from 'tg.service/apiSchema.generated';

type Bucket = components['schemas']['Bucket'];

const WEEKS_SHOWN = 53;
const BLOCK_MARGIN = 3;
const MIN_BLOCK_SIZE = 11;
const WEEKDAY_LABELS_WIDTH = 34;

const StyledContainer = styled(Box)`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 10px;
  padding: 16px 16px 8px 16px;
  min-width: 0;
`;

const levelOf = (count: number) =>
  count === 0 ? 0 : count <= 2 ? 1 : count <= 5 ? 2 : count <= 9 ? 3 : 4;

type Props = {
  buckets: Bucket[];
  year: number;
  onYearChange: (year: number) => void;
};

export const ProfileActivityCalendar = ({
  buckets,
  year,
  onYearChange,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const formatDate = useDateFormatter();
  const [mode, setMode] = useState<CommunityChartMode>('suggested');
  const currentYear = new Date().getFullYear();
  const scrollRef = useRef<HTMLDivElement>(null);

  const [measuredWidth, setMeasuredWidth] = useState<number>();
  // measured before the first paint: useWidthObserver reports a frame late, and the calendar would visibly resize
  useLayoutEffect(() => setMeasuredWidth(scrollRef.current?.clientWidth), []);
  const width = useWidthObserver({ ref: scrollRef }) ?? measuredWidth ?? 0;
  const blockSize = Math.max(
    MIN_BLOCK_SIZE,
    (width - WEEKDAY_LABELS_WIDTH + BLOCK_MARGIN) / WEEKS_SHOWN - BLOCK_MARGIN
  );

  useLayoutEffect(() => {
    // react-activity-calendar forwards its ref to the outer <article>, not the scroll container
    const scroller = scrollRef.current?.querySelector(
      '.react-activity-calendar__scroll-container'
    );
    if (scroller) {
      const now = new Date();
      const yearProgress =
        year === now.getFullYear() ? getDayOfYear(now) / getDaysInYear(now) : 1;
      scroller.scrollLeft =
        yearProgress * scroller.scrollWidth - scroller.clientWidth * 0.8;
    }
  }, [blockSize, mode, year]);

  const { data, total } = useMemo(() => {
    const firstIso = `${year}-01-01`;
    const lastIso = `${year}-12-31`;

    const activities: Activity[] = buckets
      .filter((b) => b.start >= firstIso && b.start <= lastIso)
      .map((b) => ({
        date: b.start,
        count: b[mode],
        level: levelOf(b[mode]),
      }));
    if (activities[0]?.date !== firstIso) {
      activities.unshift({ date: firstIso, count: 0, level: 0 });
    }
    if (activities[activities.length - 1]?.date !== lastIso) {
      activities.push({ date: lastIso, count: 0, level: 0 });
    }
    return {
      data: activities,
      total: activities.reduce((sum, a) => sum + a.count, 0),
    };
  }, [buckets, mode, year]);

  const modeTotalLabel = useCommunityModeTotalLabel();
  const totalLabel = (count: number) => modeTotalLabel(mode, count);

  const dayLabel = (activity: Activity) => {
    const params = {
      count: activity.count,
      date: formatDate(parseISO(activity.date), { dateStyle: 'medium' }),
    };
    switch (mode) {
      case 'accepted':
        return t(
          'community_profile_calendar_day_accepted',
          '{count, plural, one {# translation accepted} other {# translations accepted}} on {date}',
          params
        );
      case 'contributions':
        return t(
          'community_profile_calendar_day_contributions',
          '{count, plural, one {# contribution} other {# contributions}} on {date}',
          params
        );
      default:
        return t(
          'community_profile_calendar_day_suggested',
          '{count, plural, one {# suggestion} other {# suggestions}} on {date}',
          params
        );
    }
  };

  const base = {
    suggested: TRANSLATION_STATES.TRANSLATED.color,
    accepted: TRANSLATION_STATES.REVIEWED.color,
    contributions: theme.palette.tokens.secondary.main,
  }[mode];
  const scale =
    theme.palette.mode === 'dark'
      ? [
          theme.palette.emphasis[200],
          darken(base, 0.45),
          darken(base, 0.2),
          base,
          lighten(base, 0.3),
        ]
      : [
          theme.palette.emphasis[50],
          alpha(base, 0.45),
          alpha(base, 0.75),
          base,
          darken(base, 0.25),
        ];

  const monthNames = Array.from({ length: 12 }, (_, i) =>
    formatDate(new Date(2000, i, 1), { month: 'short' })
  );
  const weekdayNames = Array.from({ length: 7 }, (_, i) =>
    formatDate(new Date(2000, 0, 2 + i), { weekday: 'short' })
  );

  return (
    <StyledContainer data-cy="community-profile-calendar">
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        gap={1}
        mb={2}
      >
        <ToggleButtonGroup
          size="small"
          color="primary"
          sx={{
            '& .MuiToggleButton-root': {
              fontSize: 12,
              px: 1.5,
              py: 0.5,
            },
          }}
          exclusive
          value={mode}
          onChange={(_, value) => value && setMode(value)}
        >
          <ToggleButton
            value="suggested"
            data-cy="community-profile-calendar-suggested"
          >
            <Tooltip
              disableInteractive
              title={t(
                'community_profile_calendar_suggested_tooltip',
                'Suggestions made in community projects'
              )}
            >
              <span>{t('community_chart_suggested', 'Suggestions')}</span>
            </Tooltip>
          </ToggleButton>
          <ToggleButton
            value="accepted"
            data-cy="community-profile-calendar-accepted"
          >
            <Tooltip
              disableInteractive
              title={t(
                'community_profile_calendar_accepted_tooltip',
                'Suggestions that were accepted'
              )}
            >
              <span>{t('community_chart_accepted', 'Accepted')}</span>
            </Tooltip>
          </ToggleButton>
          <ToggleButton
            value="contributions"
            data-cy="community-profile-calendar-contributions"
          >
            <Tooltip
              disableInteractive
              title={t(
                'community_profile_calendar_contributions_tooltip',
                'Translations, suggestions and comments'
              )}
            >
              <span>{t('community_chart_contributions', 'Contributions')}</span>
            </Tooltip>
          </ToggleButton>
        </ToggleButtonGroup>
        <Box display="flex" alignItems="center" gap={1.5}>
          <Typography variant="body2" color="text.secondary">
            {totalLabel(total)}
          </Typography>
          <Select
            size="small"
            value={year}
            onChange={(e) => onYearChange(Number(e.target.value))}
            data-cy="community-profile-calendar-year"
          >
            {[currentYear, currentYear - 1].map((y) => (
              <MenuItem key={y} value={y}>
                {y}
              </MenuItem>
            ))}
          </Select>
        </Box>
      </Box>
      <Box ref={scrollRef} overflow="auto" color="text.secondary">
        <ActivityCalendar
          data={data}
          blockSize={blockSize}
          blockMargin={BLOCK_MARGIN}
          blockRadius={3}
          fontSize={12}
          weekStart={1}
          colorScheme={theme.palette.mode}
          theme={{ light: scale, dark: scale }}
          showWeekdayLabels={['mon', 'wed', 'fri']}
          showTotalCount={false}
          labels={{
            months: monthNames,
            weekdays: weekdayNames,
            legend: {
              less: t('community_profile_calendar_less', 'Less'),
              more: t('community_profile_calendar_more', 'More'),
            },
          }}
          renderBlock={(block, activity) => (
            <Tooltip disableInteractive title={dayLabel(activity)}>
              {block}
            </Tooltip>
          )}
        />
      </Box>
    </StyledContainer>
  );
};
