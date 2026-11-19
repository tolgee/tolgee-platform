import { ReactNode, useState } from 'react';
import { styled, Tooltip } from '@mui/material';

const StyledName = styled('b')`
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
`;

type Props = {
  name: string;
  children?: ReactNode;
};

export const NotificationUserName = ({ name, children }: Props) => {
  const [truncated, setTruncated] = useState(false);
  return (
    <Tooltip title={truncated ? name : ''}>
      <StyledName
        onMouseEnter={(e) =>
          setTruncated(
            e.currentTarget.scrollWidth > e.currentTarget.clientWidth
          )
        }
      >
        {children ?? name}
      </StyledName>
    </Tooltip>
  );
};
