import { styled, keyframes } from '@mui/material';

const shimmer = keyframes`
  0% { background-position: -100% 0; }
  50% { background-position: 200% 0; }
  100% { background-position: -100% 0; }
`;

export const ShimmerText = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.palette.primary.main} 0%,
    ${({ theme }) => theme.palette.primary.main} 35%,
    rgba(255, 255, 255, 0.2) 50%,
    ${({ theme }) => theme.palette.primary.main} 65%,
    ${({ theme }) => theme.palette.primary.main} 100%
  );
  background-size: 200% 100%;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: ${shimmer} 4s ease-in-out infinite;
`;
