import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { CollapsibleActivityGroup } from './CollapsibleActivityGroup';

type Group = components['schemas']['ActivityGroupSetTranslationsModel'];

export const SetTranslationsActivityGroup: FC<{
  group: Group;
}> = ({ group }) => {
  return (
    <CollapsibleActivityGroup
      expandedChildren={<ExpandedContent group={group} />}
    >
      {group.author?.name} Translated {group.data?.translationCount} strings{' '}
    </CollapsibleActivityGroup>
  );
};

const ExpandedContent: FC<{ group: Group }> = (props) => {
  // const project = useProject();
  // const getData = (page: number) =>
  //   useApiQuery({
  //     url: '/v2/projects/{projectId}/activity/group-items/create-key/{groupId}',
  //     method: 'get',
  //     path: { projectId: project.id, groupId: props.group.id },
  //     query: {
  //       page: page,
  //       size: 20,
  //     },
  //   });
  // return (
  //   <SimpleTableExpandedContent getData={getData}></SimpleTableExpandedContent>
  // );
  return null;
};
