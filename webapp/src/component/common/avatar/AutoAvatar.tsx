import { ComponentProps, FC } from 'react';
import { Skeleton, styled } from '@mui/material';
import { useAutoAvatarImgSrc } from './useAutoAvatarImgSrc';

const StyledImg = styled('img')`
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.9 : 1)}
  );
`;

export type AutoAvatarProps = {
  entityId: number | string;
  size: number;
  ownerName: string;
  ownerType: 'ORG' | 'USER' | 'PROJECT';
};

export const AutoAvatar: FC<ComponentProps<'img'> & AutoAvatarProps> = ({
  entityId,
  size,
  ownerName,
  ownerType,
  ...imgProps
}) => {
  const src = useAutoAvatarImgSrc({
    entityId,
    size,
    ownerName,
    ownerType,
  });

  return src ? (
    <StyledImg
      data-cy="auto-avatar-img"
      {...imgProps}
      src={src}
      alt={ownerName}
    />
  ) : (
    <Skeleton variant="rectangular" width={size} height={size} />
  );
};
