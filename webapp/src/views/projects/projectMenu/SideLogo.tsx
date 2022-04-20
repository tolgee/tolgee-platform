import { styled } from '@mui/material';
import { Link } from 'react-router-dom';

import { TolgeeLogo } from 'tg.component/common/icons/TolgeeLogo';

const StyledItem = styled('li')`
  display: flex;
  list-style: none;
  flex-direction: column;
`;

const StyledLink = styled(Link)`
  display: flex;
  padding: ${({ theme }) => theme.spacing(1, 0)};
  cursor: pointer;
  justify-content: center;
  min-height: ${({ theme }) => theme.mixins.toolbar.minHeight}px;
  outline: 0;
  transition: filter 0.2s ease-in-out;
  &:focus,
  &:hover {
    filter: brightness(70%);
  }
`;

const StyledTolgeeLogo = styled(TolgeeLogo)`
  color: ${({ theme: { palette } }) =>
    palette.mode === 'dark' ? palette.text.primary : palette.primary.main};
`;

type Props = {
  hidden: boolean;
};

export const SideLogo: React.FC<Props> = ({ hidden }) => {
  return (
    <StyledItem>
      <StyledLink to="/" tabIndex={hidden ? -1 : undefined}>
        <StyledTolgeeLogo fontSize="large" />
      </StyledLink>
    </StyledItem>
  );
};
