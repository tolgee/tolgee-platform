import { useEffect, useState } from 'react';
import { AutoAvatarProps } from './AutoAvatar';

function getInitialsAvatarSvg(ownerName: string, size: number) {
  return Promise.all([
    importCreateAvatarFunction(),
    import('@dicebear/avatars-initials-sprites'),
  ]).then(([crateAvatar, style]) => {
    return crateAvatar(style, {
      seed: ownerName,
      size,
      scale: 100,
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
    const svgStringPromise =
      props.type === 'INITIALS'
        ? getInitialsAvatarSvg(props.ownerName, props.size)
        : getIdenticonAvatarSvg(props.entityId, props.size);

    svgStringPromise.then((svgString) => {
      const base64 = Buffer.from(svgString).toString('base64');
      setBase64(base64);
    });
  }, [props.size, props.entityId, props.type, props.ownerName]);

  if (!base64) {
    return undefined;
  }

  return `data:image/svg+xml;base64,${base64}`;
};

function importCreateAvatarFunction() {
  return import('@dicebear/avatars').then((m) => m.createAvatar);
}
