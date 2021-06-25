import { getSvgNameByEmoji } from '@tginternal/language-util';
import { FC, ImgHTMLAttributes } from 'react';

export const getFlagPath = (hex: string) =>
  `/static/flags/${getSvgNameByEmoji(hex)}.svg`;

export const FlagImage: FC<
  ImgHTMLAttributes<HTMLImageElement> & { flagEmoji: string }
> = ({ flagEmoji, ...props }) => {
  return (
    <img
      {...props}
      loading="lazy"
      src={getFlagPath(flagEmoji || '🏁')}
      alt={flagEmoji}
    />
  );
};
