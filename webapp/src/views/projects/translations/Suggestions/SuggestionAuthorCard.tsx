import { useLayoutEffect } from 'react';
import { Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { ContributorCard } from 'tg.component/community/ContributorCard';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { useUserName } from 'tg.component/common/UserName';

type SimpleUserAccountModel = components['schemas']['SimpleUserAccountModel'];

type Props = {
  author: SimpleUserAccountModel;
  onContentChange: () => void;
};

export const SuggestionAuthorCard = ({ author, onContentChange }: Props) => {
  const project = useProject();
  const user = useUser();
  const getUserName = useUserName();
  const isMe = user?.id === author.id;
  const viewableLanguages = useTranslationsSelector((c) => c.languages);
  const { data, isLoading, error } = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors/{userId}',
    method: 'get',
    path: { projectId: project.id, userId: author.id },
    query: {},
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { staleTime: 60_000 },
  });

  useLayoutEffect(() => {
    if (!isLoading) onContentChange();
  }, [isLoading, data, error]);

  const languages =
    data?.languages
      .filter((l) => l.contributions > 0)
      .map((l) => viewableLanguages?.find((p) => p.id === l.languageId))
      .filter((l) => l !== undefined) ?? [];

  const authorNotShown = error?.code === 'user_not_found';
  const status = authorNotShown ? (
    <Typography
      variant="caption"
      color="text.secondary"
      component="div"
      data-cy="suggestion-author-card-no-stats"
    >
      <T
        keyName="community_contributor_card_no_stats"
        defaultValue="No contribution stats available"
      />
    </Typography>
  ) : undefined;

  return (
    <div data-cy="suggestion-author-card">
      <ContributorCard
        variant="popover"
        id={author.id}
        name={getUserName(author)}
        avatar={author.avatar}
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
