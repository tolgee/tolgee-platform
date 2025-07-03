import { styled, Link as MuiLink } from '@mui/material';
import { usePosthog } from 'tg.hooks/usePosthog';

const StyledMuiLink = styled(MuiLink)`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
`;

type Props = React.ComponentProps<typeof MuiLink>;

export const BannerLink = ({ ...props }: Props) => {
  const pg = usePosthog();
  return (
    <StyledMuiLink
      rel="noopener noreferrer"
      target="_blank"
      {...props}
      onClick={() => {
        pg?.capture('ANNOUNCEMENT_LINK_CLICK', { link: props.href });
      }}
    />
  );
};
