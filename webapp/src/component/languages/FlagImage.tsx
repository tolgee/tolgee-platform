import { FC, ImgHTMLAttributes } from 'react';
import { getSvgNameByEmoji } from '@tginternal/language-util';

export const getFlagPath = (hex: string) =>
  `/static/flags/${getSvgNameByEmoji(hex)}.svg`;

export const FlagImage: FC<
  ImgHTMLAttributes<HTMLImageElement> & { flagEmoji: string }
> = ({ flagEmoji, ...props }) => {
  return (
    <img
      {...props}
      loading="lazy"
      src={getFlagPath(flagEmoji || '🏳️')}
      alt={flagEmoji}
    />
  );
};
