import { useMemo } from 'react';
import { Box, useTheme } from '@mui/material';
import { useCurrentLanguage, useTranslate } from '@tolgee/react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis } from 'recharts';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const DailyActivityChart = () => {
  const project = useProject();

  const getLang = useCurrentLanguage();

  const dailyActivityLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats/daily-activity',
    method: 'get',
    path: {
      projectId: project.id,
    },
  });

  const theme = useTheme();

  const t = useTranslate();

  const dateFormatter = (timestamp) => {
    return new Date(timestamp).toLocaleDateString(getLang());
  };

  const dailyActivityData = useMemo(() => {
    if (
      dailyActivityLoadable.data &&
      Object.keys(dailyActivityLoadable.data).length > 1
    ) {
      const stringDates = Object.keys(dailyActivityLoadable.data);
      const lastDate = new Date(stringDates[stringDates.length - 1]);

      const allTimestamps = getAllTimestampsWithZeroValuesBetween(
        new Date(stringDates[0]),
        lastDate
      );

      addAllActiveDays(dailyActivityLoadable.data, allTimestamps);
      return getListOfDateActivityCountMaps(allTimestamps);
    }
    return [];
  }, [dailyActivityLoadable.data]);

  return dailyActivityData.length > 0 ? (
    <Box style={{ height: '400px' }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          width={730}
          height={250}
          data={dailyActivityData}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
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
            hide={true}
          />
          <Tooltip labelFormatter={dateFormatter} />
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
    </Box>
  ) : (
    <></>
  );
};

const getAllTimestampsWithZeroValuesBetween = (
  firstDate: Date,
  lastDate: Date
) => {
  const date = new Date(firstDate);
  date.setHours(0);
  date.setMinutes(0);
  const result = new Map<number, number>();
  while (date <= lastDate) {
    date.setHours(0);
    date.setMinutes(0);
    result.set(date.getTime(), 0);
    date.setDate(date.getDate() + 1);
  }
  return result;
};

const addAllActiveDays = (
  data: Record<string, number>,
  allTimestamps: Map<number, number>
) => {
  Object.entries(data).forEach(([dateString, activityCount]) => {
    const date = new Date(dateString);
    date.setHours(0);
    date.setMinutes(0);
    allTimestamps.set(date.getTime(), activityCount);
  });
};

function getListOfDateActivityCountMaps(allTimestamps: Map<number, number>) {
  const dailyActivityData = [] as { date: number; activityCount: number }[];
  allTimestamps.forEach((activityCount, date) =>
    dailyActivityData.push({
      date,
      activityCount,
    })
  );
  return dailyActivityData;
}
