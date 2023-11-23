import { styled, Link as MuiLink } from '@mui/material';

const StyledMuiLink = styled(MuiLink)`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
`;

type Props = React.ComponentProps<typeof MuiLink>;

export const BannerLink = (props: Props) => {
  return <StyledMuiLink rel="noopener noreferrer" target="_blank" {...props} />;
};
