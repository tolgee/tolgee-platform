import { styled } from '@mui/material';
import { DiffValue } from '../types';
import { useDateFormatter } from 'tg.hooks/useLocale';

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

type Props = {
  timestamp: number;
};

const DateChange = ({ timestamp }: Props) => {
  const formatDate = useDateFormatter();
  return <>{formatDate(timestamp)}</>;
};

export const getDateChange = (input?: DiffValue<number>) => {
  if (input?.new && input?.old) {
    return (
      <StyledDiff>
        <StyledRemoved>
          <DateChange timestamp={input.old} />
        </StyledRemoved>
        <StyledArrow>→</StyledArrow>
        <span>
          <DateChange timestamp={input.new} />
        </span>
      </StyledDiff>
    );
  }
  if (input?.new) {
    return (
      <span>
        <DateChange timestamp={input.new} />
      </span>
    );
  } else if (input?.old) {
    return (
      <StyledRemoved>
        <DateChange timestamp={input.old} />
      </StyledRemoved>
    );
  }
};
