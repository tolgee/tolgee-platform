import { useState } from 'react';
import {
  Box,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip as MuiTooltip,
  Typography,
  styled,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  Bar,
  BarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { addDays, parseISO } from 'date-fns';

import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';

type Bucket = components['schemas']['Bucket'];
export type CommunityChartMode = 'suggested' | 'accepted' | 'contributions';

export const useCommunityModeTotalLabel = () => {
  const { t } = useTranslate();
  return (mode: CommunityChartMode, count: number) => {
    switch (mode) {
      case 'accepted':
        return t(
          'community_chart_accepted_total',
          '{count, plural, one {# translation accepted} other {# translations accepted}}',
          { count }
        );
      case 'contributions':
        return t(
          'community_chart_contributions_total',
          '{count, plural, one {# contribution} other {# contributions}}',
          { count }
        );
      default:
        return t(
          'community_chart_suggested_total',
          '{count, plural, one {# suggestion} other {# suggestions}}',
          { count }
        );
    }
  };
};

const StyledContainer = styled(Box)`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 10px;
  padding: ${({ theme }) => theme.spacing(2, 2, 1)};
  min-width: 0;
`;

const StyledTooltip = styled(Box)`
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(0.75, 1.25)};
`;

type Props = {
  buckets: Bucket[];
  granularity: 'day' | 'week';
  axisRange?: { from: string; to: string };
};

export const CommunityActivityChart = ({
  buckets,
  granularity,
  axisRange,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const maxTicks = useMediaQuery(theme.breakpoints.down('sm')) ? 5 : 12;
  const tickStep = Math.max(1, Math.ceil(buckets.length / maxTicks));
  const lastIndex = buckets.length - 1;
  const tickIndexes = buckets
    .map((_, i) => i)
    .filter((i) => i % tickStep === 0 && lastIndex - i >= tickStep * 0.75);
  if (lastIndex >= 0) {
    tickIndexes.push(lastIndex);
  }
  const ticks = tickIndexes.map((i) => buckets[i].start);
  const formatDate = useDateFormatter();
  const [mode, setMode] = useState<CommunityChartMode>('suggested');

  const total = buckets.reduce((sum, b) => sum + b[mode], 0);
  const colors: Record<CommunityChartMode, string> = {
    suggested: theme.palette.tokens.state.translated,
    accepted: theme.palette.tokens.state.reviewed,
    contributions: theme.palette.tokens.secondary.main,
  };
  const labels: Record<CommunityChartMode, string> = {
    suggested: t('community_chart_suggested', 'Suggestions'),
    accepted: t('community_chart_accepted', 'Accepted'),
    contributions: t('community_chart_contributions', 'Contributions'),
  };
  const formatTick = (start: string) => {
    const date =
      axisRange && start === buckets[0]?.start
        ? axisRange.from
        : axisRange && start === buckets[lastIndex]?.start
        ? axisRange.to
        : start;
    return formatDate(parseISO(date), { day: 'numeric', month: 'numeric' });
  };
  const formatPeriod = (start: string) => {
    const from = parseISO(start);
    if (granularity === 'day') {
      return formatDate(from);
    }
    return t('community_chart_week', 'Week {from} – {to}', {
      from: formatDate(from),
      to: formatDate(addDays(from, 6)),
    });
  };
  const totalLabel = useCommunityModeTotalLabel();
  const countLabel = (count: number) => totalLabel(mode, count);

  return (
    <StyledContainer data-cy="community-activity-chart">
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
              fontSize: theme.typography.caption.fontSize,
              px: 1.5,
              py: 0.5,
            },
          }}
          exclusive
          value={mode}
          onChange={(_, value) => value && setMode(value)}
        >
          <ToggleButton value="suggested" data-cy="community-chart-suggested">
            <MuiTooltip
              disableInteractive
              title={t(
                'community_chart_suggested_tooltip',
                'All suggestions from the community'
              )}
            >
              <span>{labels.suggested}</span>
            </MuiTooltip>
          </ToggleButton>
          <ToggleButton value="accepted" data-cy="community-chart-accepted">
            <MuiTooltip
              disableInteractive
              title={t(
                'community_chart_accepted_tooltip',
                'Accepted community suggestions'
              )}
            >
              <span>{labels.accepted}</span>
            </MuiTooltip>
          </ToggleButton>
          <ToggleButton
            value="contributions"
            data-cy="community-chart-contributions"
          >
            <MuiTooltip
              disableInteractive
              title={t(
                'community_chart_contributions_tooltip',
                'Translations, reviews, comments and suggestions'
              )}
            >
              <span>{labels.contributions}</span>
            </MuiTooltip>
          </ToggleButton>
        </ToggleButtonGroup>
        <Typography variant="caption" color="tokens.icon.secondary">
          {countLabel(total)}
        </Typography>
      </Box>
      <Box height={160}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={buckets}
            margin={{ top: 5, right: 16, left: 16, bottom: 0 }}
          >
            <XAxis
              dataKey="start"
              ticks={ticks}
              interval={0}
              tickFormatter={formatTick}
              tick={{
                fontSize: theme.typography.caption.fontSize,
                fill: theme.palette.text.secondary,
              }}
              tickLine={false}
            />
            <YAxis hide allowDecimals={false} />
            <Tooltip
              cursor={false}
              content={({ active, payload }) =>
                active && payload?.length ? (
                  <StyledTooltip>
                    <Typography variant="caption" color="text.secondary">
                      {formatPeriod(payload[0].payload.start)}
                    </Typography>
                    <Typography variant="body2">
                      {countLabel(payload[0].payload[mode])}
                    </Typography>
                  </StyledTooltip>
                ) : null
              }
            />
            <Bar
              dataKey={mode}
              fill={colors[mode]}
              fillOpacity={isDark ? 0.85 : 0.6}
              activeBar={{ fillOpacity: 1 }}
              maxBarSize={buckets.length <= 20 ? 40 : 20}
              background={{
                fill: theme.palette.tokens.state.untranslated,
                fillOpacity: isDark ? 0.1 : 0.3,
                radius: 2,
              }}
              radius={[2, 2, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </StyledContainer>
  );
};
