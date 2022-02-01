import { ComponentProps, FC, useEffect, useState } from 'react';
import { Skeleton } from '@material-ui/lab';

export const AutoAvatar: FC<
  ComponentProps<'img'> & { entityId: number | string; size: number }
> = ({ entityId, size, ...imgProps }) => {
  const [base64, setBase64] = useState(undefined as string | undefined);

  useEffect(() => {
    Promise.all([
      import('@dicebear/avatars').then((m) => m.createAvatar),
      import('@dicebear/avatars-identicon-sprites'),
    ]).then(([crateAvatar, style]) => {
      const svgString = crateAvatar(style, {
        seed: entityId.toString(),
        size,
        radius: 50,
        scale: 70,
      });

      const base64 = Buffer.from(svgString).toString('base64');
      setBase64(base64);
    });
  }, [size, entityId]);
  // @ts-ignore

  return base64 ? (
    <div style={{ backgroundColor: 'rgb(239,239,239)', display: 'flex' }}>
      <img
        data-cy="auto-avatar-img"
        {...imgProps}
        src={`data:image/svg+xml;base64,${base64}`}
        alt={'User Avatar'}
      />
    </div>
  ) : (
    <Skeleton variant="rect" width={size} height={size} />
  );
};
