import { keyframes, styled } from '@mui/material';
import { Stars } from './CustomIcons';

const twinkle = keyframes`
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(0.98); }
`;

const pulse = keyframes`
  0%, 100% { filter: drop-shadow(0 0 1px currentColor); }
  50% { filter: drop-shadow(0 0 3px currentColor); }
`;

export const AnimatedStars = styled(Stars)`
  animation: ${twinkle} 1.5s ease-in-out infinite,
    ${pulse} 2s ease-in-out infinite;
  color: ${({ theme }) => theme.palette.primary.main};
`;
