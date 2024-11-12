import { styled, useTheme } from '@mui/material';
import { QsFinished } from 'tg.component/CustomIcons';

const RADIUS = 45;
const CIRCUIT = RADIUS * Math.PI * 2;

const StyledCircleBackground = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke: ${({ theme }) => theme.palette.quickStart.progressBackground};
`;

const StyledCircleContent = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke-linecap: round;
  transform-origin: 50% 50%;
  stroke-dasharray: ${CIRCUIT};
  stroke: ${({ theme }) => theme.palette.quickStart.circleSuccess};
  transition: stroke-dashoffset 0.2s ease-in-out;
`;

type Props = {
  percent: number;
  size?: number;
};

export const QuickStartProgress = ({ percent, size = 28 }: Props) => {
  const theme = useTheme();
  const normalized = percent > 1 ? 1 : percent < 0 ? 0 : percent;

  const fullLength = percent > 1 ? percent : 1;
  const progressLength = CIRCUIT - (normalized / fullLength) * CIRCUIT;
  const rotation = 0;

  if (percent === 1) {
    return (
      <QsFinished
        fillOpacity={0}
        style={{ color: theme.palette.quickStart.circleSuccess }}
      />
    );
  }

  return (
    <svg viewBox="0 0 114 114" style={{ width: size, height: size }}>
      <StyledCircleBackground cx="57" cy="57" r={RADIUS} />
      <StyledCircleContent
        cx="57"
        cy="57"
        r={RADIUS}
        sx={{
          strokeDashoffset: progressLength,
          transform: `rotate(${-90 + rotation}deg)`,
        }}
      />
    </svg>
  );
};
