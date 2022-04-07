import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { Tooltip } from '@material-ui/core';

export const ActivityValue: FC<{ maxLength?: number }> = (props) => {
  let children = props.children;
  let tooltip = null as null | string;
  const maxLength = props.maxLength || 50;
  if (
    Array.isArray(children) &&
    children.length === 1 &&
    typeof children[0] === 'string'
  ) {
    const string = children[0];

    if (string.length > maxLength) {
      children = [string.substr(0, maxLength - 3) + '...'];
      tooltip = string;
    }
  }

  const Wrapper: FC = (props) =>
    tooltip ? (
      <Tooltip title={tooltip}>{props.children as any}</Tooltip>
    ) : (
      <>{props.children}</>
    );

  return (
    <Wrapper>
      <span
        style={{
          backgroundColor: '#f1f1f1',
          padding: '3px 6px',
          borderRadius: 5,
        }}
      >
        {children}
      </span>
    </Wrapper>
  );
};

export const prepareValue = (value: any) => {
  const string = '' + (value || '??');
  return string;
};

export const getOnlyModifiedEntity = (props: {
  item: components['schemas']['ProjectActivityModel'];
  entity: string;
}) => {
  return props.item.modifiedEntities?.[props.entity]?.[0];
};

export const getAllModifiedEntites = (props: {
  item: components['schemas']['ProjectActivityModel'];
  entity: string;
}) => {
  return props.item.modifiedEntities?.[props.entity];
};

export const renderEntityLanguage = (languageDescription) => {
  const name = languageDescription.data['name'];
  const flagEmoji = languageDescription.data['flagEmoji'];
  return `${name}${flagEmoji ? ` ${flagEmoji}` : ''}`;
};
