import { ReactNode, useState } from 'react';
import { Box, BoxProps, Tooltip } from '@mui/material';

type Props = BoxProps & {
  tooltip: ReactNode;
};

export const TruncatedTooltip = ({
  tooltip,
  children,
  sx,
  ...boxProps
}: Props) => {
  const [truncated, setTruncated] = useState(false);
  return (
    <Tooltip
      title={truncated ? tooltip : ''}
      componentsProps={{ tooltip: { style: { maxWidth: 400 } } }}
    >
      <Box
        {...boxProps}
        sx={{
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          ...sx,
        }}
        onMouseEnter={(e) =>
          setTruncated(
            e.currentTarget.scrollWidth > e.currentTarget.clientWidth
          )
        }
      >
        {children}
      </Box>
    </Tooltip>
  );
};
