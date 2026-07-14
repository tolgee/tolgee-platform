import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityGroupCard } from './ActivityGroupCard';
import { GenericGroupExpandedContent } from './GenericGroupExpandedContent';
import { CreateKeysExpandedContent } from './groupTypeComponents/CreateKeysActivityGroup';
import { CreateProjectExpandedContent } from './groupTypeComponents/CreateProjectActivityGroup';
import { groupsConfiguration } from './groupsConfiguration';

type ActivityGroupModel = components['schemas']['ActivityGroupModel'];

export const ActivityGroupItem: FC<{
  item: ActivityGroupModel;
}> = ({ item }) => {
  const label = groupsConfiguration[item.type]?.label ?? item.type;
  const data = ('data' in item ? item.data : undefined) as
    | Record<string, any>
    | undefined;

  switch (item.type) {
    case 'CREATE_PROJECT':
      return (
        <ActivityGroupCard
          item={item}
          expandedContent={<CreateProjectExpandedContent data={data} />}
        >
          {label}
        </ActivityGroupCard>
      );
    case 'CREATE_KEY':
      return (
        <ActivityGroupCard
          item={item}
          count={data?.keyCount}
          expandedContent={<CreateKeysExpandedContent groupId={item.id} />}
        >
          {label}
        </ActivityGroupCard>
      );
    case 'SET_TRANSLATIONS':
      return (
        <ActivityGroupCard
          item={item}
          count={data?.translationCount}
          expandedContent={<GenericGroupExpandedContent groupId={item.id} />}
        >
          {label}
        </ActivityGroupCard>
      );
    default:
      return (
        <ActivityGroupCard
          item={item}
          count={sumCounts(data)}
          expandedContent={<GenericGroupExpandedContent groupId={item.id} />}
        >
          {label}
        </ActivityGroupCard>
      );
  }
};

function sumCounts(data: Record<string, any> | undefined): number | undefined {
  const counts = data?.counts as Record<string, number> | undefined;
  if (!counts) {
    return undefined;
  }
  return Object.values(counts).reduce((a, b) => a + b, 0);
}
