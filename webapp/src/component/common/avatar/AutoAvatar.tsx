import { ComponentProps, FC } from 'react';
import { Skeleton } from '@material-ui/lab';
import { useAutoAvatarImgSrc } from './useAutoAvatarImgSrc';

export type AutoAvatarType = 'INITIALS' | 'IDENTICON';

export type AutoAvatarProps = {
  entityId: number | string;
  size: number;
  ownerName: string;
  type: AutoAvatarType;
};

export const AutoAvatar: FC<ComponentProps<'img'> & AutoAvatarProps> = ({
  entityId,
  size,
  type,
  ownerName,
  ...imgProps
}) => {
  const src = useAutoAvatarImgSrc({ entityId, size, type, ownerName });

  return src ? (
    <div style={{ backgroundColor: 'rgb(239,239,239)', display: 'flex' }}>
      <img data-cy="auto-avatar-img" {...imgProps} src={src} alt={ownerName} />
    </div>
  ) : (
    <Skeleton variant="rect" width={size} height={size} />
  );
};
