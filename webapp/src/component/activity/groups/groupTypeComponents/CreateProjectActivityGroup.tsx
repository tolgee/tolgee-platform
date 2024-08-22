import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { CollapsibleActivityGroup } from './CollapsibleActivityGroup';

type Group = components['schemas']['ActivityGroupCreateProjectModel'];

export const CreateProjectActivityGroup: FC<{
  group: Group;
}> = ({ group }) => {
  return (
    <CollapsibleActivityGroup
      expandedChildren={<ExpandedContent group={group} />}
    >
      {group.author?.name} Created project {group.data?.name}
    </CollapsibleActivityGroup>
  );
};

const ExpandedContent: FC<{ group: Group }> = (props) => {
  return <pre>{JSON.stringify(props.group, null, 2)}</pre>;
};
