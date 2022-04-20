import { ComponentProps, FC } from 'react';
import { Skeleton, styled } from '@mui/material';
import { useAutoAvatarImgSrc } from './useAutoAvatarImgSrc';

export type AutoAvatarType = 'INITIALS' | 'IDENTICON';

const StyledImg = styled('img')`
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.9 : 1)}
  );
`;

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
      <StyledImg
        data-cy="auto-avatar-img"
        {...imgProps}
        src={src}
        alt={ownerName}
      />
    </div>
  ) : (
    <Skeleton variant="rectangular" width={size} height={size} />
  );
};
