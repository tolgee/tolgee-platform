import { useEffect, useState } from 'react';
import { AutoAvatarProps } from './AutoAvatar';
import { colors } from '@mui/material';

const COLORS_LIGHT = [
  colors.amber[300],
  colors.blue[300],
  colors.blueGrey[300],
  colors.brown[300],
  colors.common[300],
  colors.cyan[300],
  colors.deepOrange[300],
  colors.deepPurple[300],
  colors.green[300],
  colors.grey[300],
  colors.indigo[300],
  colors.lightBlue[300],
  colors.lightGreen[300],
  colors.lime[300],
  colors.orange[300],
  colors.pink[300],
  colors.purple[300],
  colors.red[300],
  colors.teal[300],
];

const COLORS_DARK = [
  colors.amber[700],
  colors.blue[700],
  colors.blueGrey[700],
  colors.brown[700],
  colors.common[700],
  colors.cyan[700],
  colors.deepOrange[700],
  colors.deepPurple[700],
  colors.green[700],
  colors.grey[700],
  colors.indigo[700],
  colors.lightBlue[700],
  colors.lightGreen[700],
  colors.lime[700],
  colors.orange[700],
  colors.pink[700],
  colors.purple[700],
  colors.red[700],
  colors.teal[700],
];

function getInitialsAvatarSvg(ownerName: string, size: number, light: boolean) {
  return Promise.all([
    importCreateAvatarFunction(),
    import('@dicebear/initials'),
  ]).then(([crateAvatar, style]) => {
    return crateAvatar(style, {
      seed: ownerName,
      size,
      scale: 100,
      backgroundColor: light ? COLORS_LIGHT : COLORS_DARK,
    });
  });
}

function getIdenticonAvatarSvg(entityId: number | string, size: number) {
  return Promise.all([
    importCreateAvatarFunction(),
    import('@dicebear/identicon'),
  ]).then(([crateAvatar, style]) => {
    return crateAvatar(style, {
      seed: entityId.toString(),
      size: size,
      scale: 70,
    });
  });
}

export const useAutoAvatarImgSrc = (props: AutoAvatarProps) => {
  const [url, setUrl] = useState(undefined as string | undefined);

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

    svgStringPromise.then((svg) => {
      if (mounted) setUrl(svg.toDataUriSync());
    });
    return () => {
      mounted = false;
    };
  }, [props.size, props.entityId, props.ownerName, props.ownerType]);

  return url;
};

function importCreateAvatarFunction() {
  return import('@dicebear/core').then((m) => m.createAvatar);
}
