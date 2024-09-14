import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { CreateProjectActivityGroup } from './groupTypeComponents/CreateProjectActivityGroup';
import { CollapsibleActivityGroup } from './groupTypeComponents/CollapsibleActivityGroup';
import { CreateKeysActivityGroup } from './groupTypeComponents/CreateKeysActivityGroup';
import { SetTranslationsActivityGroup } from './groupTypeComponents/SetTranslationsActivityGroup';

export const ActivityGroupItem: FC<{
  item: components['schemas']['ActivityGroupModel'];
}> = (props) => {
  switch (props.item.type) {
    case 'CREATE_PROJECT':
      return <CreateProjectActivityGroup group={props.item} />;
    case 'CREATE_KEY':
      return <CreateKeysActivityGroup group={props.item} />;
    case 'SET_TRANSLATIONS':
      return <SetTranslationsActivityGroup group={props.item} />;
    default:
      return (
        <CollapsibleActivityGroup>{props.item.type}</CollapsibleActivityGroup>
      );
  }
};
