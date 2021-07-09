import { BoxProps } from '@material-ui/core';
import { Box } from '@material-ui/core';

type Props = BoxProps;

export const CellControls: React.FC<Props> = ({ children, ...props }) => {
  return (
    <Box position="absolute" top={0} right={0} display="flex" {...props}>
      {children}
    </Box>
  );
};
