import { useMemo, useState } from 'react';
import {
  Box,
  Typography,
  Select,
  MenuItem,
  useTheme,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

// seconds * minutes * hours * milliseconds = 1 day
const DAY = 60 * 60 * 24 * 1000;

const StyledContainer = styled(Box)`
  height: 400px;
  width: 100%;
  contain: size;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 10px;
  padding: 10px 0px;

  & .label {
    background: ${({ theme }) => theme.palette.background.default};
  }
`;

const StyledSelect = styled(Select)`
  margin-left: 10px;
`;

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  margin: 0px 12px 8px 12px;
  align-items: center;
`;

type Props = {
  dailyActivity?: { [key: string]: number };
};

export const DailyActivityChart: React.FC<Props> = ({ dailyActivity }) => {
  const language = useCurrentLanguage();

  const [period, setPeriod] = useState<'week' | 'month' | 'year' | 'all'>(
    'month'
  );

  const theme = useTheme();

  const { t } = useTranslate();

  const dateFormatter = (timestamp) => {
    return new Date(timestamp).toLocaleDateString(language);
  };

  const dailyActivityData = useMemo(() => {
    if (dailyActivity) {
      const numberOfDays =
        period === 'week' ? 7 : period === 'month' ? 30 : 365;
      const periodStart = new Date(
        new Date().setDate(new Date().getDate() - numberOfDays)
      );
      const firstDate = new Date(Object.keys(dailyActivity)[0]);

      const startDate =
        period === 'all'
          ? firstDate < periodStart
            ? firstDate
            : periodStart
          : periodStart;

      const lastDate = new Date();

      const allDates = getAllDates(startDate, lastDate);

      return getDailyActivity(dailyActivity, allDates);
    }
    return [];
  }, [dailyActivity, period]);

  return dailyActivityData.length > 0 ? (
    <StyledContainer style={{ height: '450px' }}>
      <StyledHeader>
        <Typography variant="h5">
          <T keyName="dashboard_activity_chart_title" />
        </Typography>

        <StyledSelect
          size="small"
          value={period}
          onChange={(e) => setPeriod(e.target.value as any)}
        >
          <MenuItem value="week">
            <T keyName="dashboard_activity_chart_period_week" />
          </MenuItem>
          <MenuItem value="month">
            <T keyName="dashboard_activity_chart_period_month" />
          </MenuItem>
          <MenuItem value="year">
            <T keyName="dashboard_activity_chart_period_year" />
          </MenuItem>
          <MenuItem value="all">
            <T keyName="dashboard_activity_chart_period_all" />
          </MenuItem>
        </StyledSelect>
      </StyledHeader>

      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={dailyActivityData}
          margin={{ top: 5, right: 30, left: 20, bottom: 50 }}
        >
          <XAxis
            domain={[
              dailyActivityData[0].date,
              dailyActivityData[dailyActivityData.length - 1].date,
            ]}
            dataKey="date"
            scale="time"
            type="number"
            tickFormatter={dateFormatter}
            hide={false}
            angle={30}
            height={50}
            tickMargin={20}
          />
          <YAxis dataKey="activityCount" type="number" width={25} />
          <Tooltip
            contentStyle={{ background: theme.palette.background.paper }}
            labelFormatter={dateFormatter}
          />
          <Line
            dot={false}
            stroke={theme.palette.primary.main}
            strokeWidth={2}
            type="monotone"
            dataKey="activityCount"
            name={t(
              'project_dashboard_chart_daily_activity_count_tooltip',
              'Daily Activity'
            )}
          />
        </LineChart>
      </ResponsiveContainer>
    </StyledContainer>
  ) : null;
};

const getAllDates = (firstDate: Date, lastDate: Date) => {
  let date = new Date(firstDate);
  const result: string[] = [];
  while (date <= lastDate) {
    result.push(
      `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
        2,
        '0'
      )}-${String(date.getDate()).padStart(2, '0')}`
    );
    date = new Date(date.getTime() + DAY);
  }
  return result;
};

const getDailyActivity = (data: Record<string, number>, allDates: string[]) => {
  const result: { date: number; activityCount: number }[] = allDates.map(
    (date) => ({
      date: new Date(date).getTime(),
      activityCount: data[date] || 0,
    })
  );
  return result;
};
