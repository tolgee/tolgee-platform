import { FC, ImgHTMLAttributes } from 'react';
import { styled } from '@mui/material';
import { getFlagPath } from './utils';

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
