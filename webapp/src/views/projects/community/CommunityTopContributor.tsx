import { useRef } from 'react';
import { Box, Tooltip } from '@mui/material';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';
import { ContributorPopoverCard } from 'tg.component/community/ContributorPopoverCard';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { CommunityTopContributors } from 'tg.views/projects/community/useCommunityTopContributors';

type Props = {
  contributor: CommunityTopContributors['contributors'][number];
};

export const CommunityTopContributor = ({ contributor }: Props) => {
  const project = useProject();
  const popperRef = useRef<any>(null);
  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000, sort: ['tag'] },
  });

  return (
    <Tooltip
      PopperProps={{ popperRef }}
      componentsProps={{ tooltip: { sx: { maxWidth: 'none' } } }}
      title={
        <ContributorPopoverCard
          contributor={contributor}
          projectLanguages={languages.data?._embedded?.languages}
          onContentChange={() => popperRef.current?.update()}
        />
      }
    >
      <Box
        display="flex"
        alignItems="center"
        gap={1}
        justifySelf="start"
        data-cy="community-top-contributor"
        data-cy-user-id={contributor.id}
      >
        <AvatarImg
          owner={{
            name: contributor.name,
            avatar: contributor.avatar,
            type: 'USER',
            id: contributor.id,
          }}
          size={24}
        />
        <UserName name={contributor.name} />
      </Box>
    </Tooltip>
  );
};
