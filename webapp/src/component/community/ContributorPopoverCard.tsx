import { useLayoutEffect } from 'react';
import { Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { ContributorCard } from 'tg.component/community/ContributorCard';
import { SimpleUser, useUserName } from 'tg.component/common/UserName';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  contributor: SimpleUser & {
    id: number;
    avatar?: components['schemas']['Avatar'];
  };
  projectLanguages: LanguageModel[] | undefined;
  onContentChange: () => void;
};

export const ContributorPopoverCard = ({
  contributor,
  projectLanguages,
  onContentChange,
}: Props) => {
  const project = useProject();
  const user = useUser();
  const getUserName = useUserName();
  const isMe = user?.id === contributor.id;
  const { data, isLoading, error } = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors/{userId}',
    method: 'get',
    path: { projectId: project.id, userId: contributor.id },
    query: {},
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { staleTime: 60_000 },
  });

  useLayoutEffect(() => {
    if (!isLoading) onContentChange();
  }, [isLoading, data, error, projectLanguages]);

  const languages =
    data?.languages
      .filter((l) => l.contributions > 0)
      .map((l) => projectLanguages?.find((p) => p.id === l.languageId))
      .filter((l) => l !== undefined) ?? [];

  const contributorNotShown = error?.code === 'user_not_found';
  const status = contributorNotShown ? (
    <Typography
      variant="caption"
      color="text.secondary"
      component="div"
      data-cy="contributor-popover-card-no-stats"
    >
      <T
        keyName="community_contributor_card_no_stats"
        defaultValue="No contribution stats available"
      />
    </Typography>
  ) : undefined;

  return (
    <div data-cy="contributor-popover-card">
      <ContributorCard
        variant="popover"
        id={contributor.id}
        name={getUserName(contributor)}
        avatar={contributor.avatar}
        isMe={isMe}
        languages={languages}
        contributions={data?.contributions}
        suggested={data?.suggested}
        accepted={data?.accepted}
        declined={data?.declined}
        firstContributionAt={data?.firstContributionAt}
        lastContributionAt={data?.lastContributionAt}
        status={status}
        loading={isLoading}
        profilePublic={!!data?.profilePublic}
      />
    </div>
  );
};
