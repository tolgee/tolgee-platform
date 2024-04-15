import { FC, ImgHTMLAttributes } from 'react';
import { getSvgNameByEmoji } from '@tginternal/language-util';
import { styled } from '@mui/material';

export type FlagInfo = {
  code: string;
  name: string;
  emoji: string;
};

const StyledImg = styled('img')`
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.9 : 1)}
  );
`;

export const getFlagPath = (hex: string) => {
  let flagName: string;
  try {
    flagName = getSvgNameByEmoji(hex);
  } catch (e) {
    flagName = getSvgNameByEmoji('🏳️');
  }
  return `/flags/${flagName}.svg`;
};

type Props = ImgHTMLAttributes<HTMLImageElement> & {
  flagEmoji: string;
};

export const FlagImage: FC<Props> = ({ flagEmoji, ...props }) => {
  return (
    <StyledImg
      {...props}
      loading="lazy"
      src={getFlagPath(flagEmoji)}
      alt={flagEmoji}
    />
  );
};
