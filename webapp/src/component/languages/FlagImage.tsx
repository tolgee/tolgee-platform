import { FC, ImgHTMLAttributes } from 'react';
import { getSvgNameByEmoji } from '@tginternal/language-util';
import { styled } from '@mui/material';

const StyledImg = styled('img')`
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.9 : 1)}
  );
`;

export const getFlagPath = (hex: string) =>
  `/static/flags/${getSvgNameByEmoji(hex)}.svg`;

export const FlagImage: FC<
  React.PropsWithChildren<
    ImgHTMLAttributes<HTMLImageElement> & { flagEmoji: string }
  >
> = ({ flagEmoji, ...props }) => {
  return (
    <StyledImg
      {...props}
      loading="lazy"
      src={getFlagPath(flagEmoji || '🏳️')}
      alt={flagEmoji}
    />
  );
};
