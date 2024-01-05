import { styled, Link } from '@mui/material';

const StyledLink = styled(Link)`
  display: inline;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 0.2em;
  text-decoration-thickness: 1%;
  color: unset;
  &:hover,
  &:active {
    color: ${({ theme }) => theme.palette.primaryText};
  }
  transition: color 200ms ease-in-out;
`;

type Props = {
  href: string;
  newTab?: boolean;
};

export const FeatureLink: React.FC<Props> = ({ children, href, newTab }) => {
  if (newTab) {
    return (
      <StyledLink href={href} target="_blank" rel="noreferrer noopener">
        {children}
      </StyledLink>
    );
  } else {
    return <StyledLink href={href}>{children}</StyledLink>;
  }
};
