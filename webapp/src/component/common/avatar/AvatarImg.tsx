import { AutoAvatar, AutoAvatarType } from './AutoAvatar';
import React from 'react';
import { AvatarOwner } from './ProfileAvatar';

export const AvatarImg = (props: {
  size: number;
  owner: AvatarOwner;
  autoAvatarType: AutoAvatarType;
  circle?: boolean;
}) => {
  const avatarPath =
    props.size <= 50
      ? props.owner.avatar?.thumbnail
      : props.owner.avatar?.large;

  return (
    <div
      style={{
        borderRadius: props.circle ? '50%' : 5,
        overflow: 'hidden',
        display: 'flex',
      }}
    >
      {avatarPath ? (
        <img
          data-cy={'avatar-image'}
          src={avatarPath}
          alt={props.owner.name || 'Avatar'}
          style={{ width: props.size, height: props.size }}
        />
      ) : (
        <AutoAvatar
          entityId={`${props.owner.type}-${props.owner.id}`}
          type={props.autoAvatarType}
          size={props.size}
          ownerName={props.owner.name || 'AVATAR'}
        />
      )}
    </div>
  );
};
