import { styled } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

const StyledCloseIcon = styled(XClose)`
  cursor: pointer;
  padding: 2px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  onClick?: React.MouseEventHandler<SVGElement>;
};

export const CloseButton: React.FC<Props> = ({ onClick }) => {
  return (
    <StyledCloseIcon
      role="button"
      data-cy="translations-tag-close"
      onClick={onClick}
    />
  );
};
