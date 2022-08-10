import { useEffect, useState } from 'react';
import { AutoAvatarProps } from './AutoAvatar';

function getInitialsAvatarSvg(ownerName: string, size: number, light: boolean) {
  return Promise.all([
    importCreateAvatarFunction(),
    import('@dicebear/avatars-initials-sprites'),
  ]).then(([crateAvatar, style]) => {
    return crateAvatar(style, {
      seed: ownerName,
      size,
      scale: 100,
      backgroundColorLevel: light ? 300 : 700,
    });
  });
}

function getIdenticonAvatarSvg(entityId: number | string, size: number) {
  return Promise.all([
    importCreateAvatarFunction(),
    import('@dicebear/avatars-identicon-sprites'),
  ]).then(([crateAvatar, style]) => {
    return crateAvatar(style, {
      seed: entityId.toString(),
      size: size,
      scale: 70,
    });
  });
}

export const useAutoAvatarImgSrc = (props: AutoAvatarProps) => {
  const [base64, setBase64] = useState(undefined as string | undefined);

  useEffect(() => {
    let mounted = true;
    const svgStringPromise =
      props.ownerType === 'USER'
        ? getIdenticonAvatarSvg(props.entityId, props.size)
        : getInitialsAvatarSvg(
            props.ownerName,
            props.size,
            props.ownerType === 'PROJECT'
          );

    svgStringPromise.then((svgString) => {
      const base64 = Buffer.from(svgString).toString('base64');
      if (mounted) {
        setBase64(base64);
      }
    });
    return () => {
      mounted = false;
    };
  }, [props.size, props.entityId, props.ownerName, props.ownerType]);

  if (!base64) {
    return undefined;
  }

  return `data:image/svg+xml;base64,${base64}`;
};

function importCreateAvatarFunction() {
  return import('@dicebear/avatars').then((m) => m.createAvatar);
}
