import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { CollapsibleActivityGroup } from './CollapsibleActivityGroup';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { SimpleTableExpandedContent } from '../SimpleTableExpandedContent';

type Group = components['schemas']['ActivityGroupCreateKeyModel'];

export const CreateKeysActivityGroup: FC<{
  group: Group;
}> = ({ group }) => {
  return (
    <CollapsibleActivityGroup
      expandedChildren={<ExpandedContent group={group} />}
    >
      {group.author?.name} Created {group.data?.keyCount} keys{' '}
    </CollapsibleActivityGroup>
  );
};

const ExpandedContent: FC<{ group: Group }> = (props) => {
  const project = useProject();
  const getData = (page: number) =>
    useApiQuery({
      url: '/v2/projects/{projectId}/activity/group-items/create-key/{groupId}',
      method: 'get',
      path: { projectId: project.id, groupId: props.group.id },
      query: {
        page: page,
        size: 20,
      },
    });
  return (
    <SimpleTableExpandedContent getData={getData}></SimpleTableExpandedContent>
  );
};
