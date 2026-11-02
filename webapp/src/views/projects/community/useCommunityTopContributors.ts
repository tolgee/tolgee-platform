import { useMemo } from 'react';
import { formatISO, subDays, subYears } from 'date-fns';

import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type CommunityContributorModel =
  components['schemas']['CommunityContributorModel'];

export type CommunityTopContributors = {
  period: 'month' | 'year';
  contributors: CommunityContributorModel[];
};

export const useCommunityTopContributors = (
  projectId: number
): CommunityTopContributors | undefined => {
  const [monthAgo, yearAgo] = useMemo(() => {
    const now = new Date();
    return [subDays(now, 29), subYears(now, 1)].map((date) =>
      formatISO(date, { representation: 'date' })
    );
  }, []);

  const recentTop = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors',
    method: 'get',
    path: { projectId },
    query: { size: 3, sort: ['contributions,desc'], from: monthAgo },
  });
  const recentHasTop3 = (recentTop.data?.page?.totalElements ?? 0) >= 3;

  const yearTop = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors',
    method: 'get',
    path: { projectId },
    query: { size: 3, sort: ['contributions,desc'], from: yearAgo },
    options: { enabled: recentTop.isSuccess && !recentHasTop3 },
  });

  if (!recentTop.isSuccess) return undefined;
  if (recentHasTop3) {
    return {
      period: 'month',
      contributors: recentTop.data._embedded?.contributors ?? [],
    };
  }
  if (!yearTop.isSuccess) return undefined;
  return {
    period: 'year',
    contributors: yearTop.data._embedded?.contributors ?? [],
  };
};
