import { Box } from '@material-ui/core';
import { CloseButton } from './CloseButton';
import { Wrapper } from './Wrapper';

type Props = {
  name: string;
  onDelete?: React.MouseEventHandler<SVGElement>;
};

export const Tag: React.FC<Props> = ({ name, onDelete }) => {
  return (
    <Wrapper>
      <Box
        flexShrink={1}
        overflow="hidden"
        textOverflow="ellipsis"
        whiteSpace="nowrap"
      >
        {name}
      </Box>
      <CloseButton onClick={onDelete} />
    </Wrapper>
  );
};
