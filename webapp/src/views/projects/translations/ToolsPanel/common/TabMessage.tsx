import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  margin: ${({ theme }) => theme.spacing(1, 1.25)};
  color: ${({ theme }) => theme.palette.text.disabled};
  font-style: italic;
`;

type Props = {
  children: React.ReactNode;
  'data-cy'?: string;
};

export const TabMessage: React.FC<React.PropsWithChildren<Props>> = ({
  children,
  'data-cy': dataCy,
}) => {
  return (
    <StyledWrapper onMouseDown={(e) => e.preventDefault()} data-cy={dataCy}>
      {children}
    </StyledWrapper>
  );
};
