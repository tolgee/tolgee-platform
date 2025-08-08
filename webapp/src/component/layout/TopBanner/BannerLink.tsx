import { styled, Link as MuiLink } from '@mui/material';
import { useReportEvent } from 'tg.hooks/useReportEvent';

const StyledMuiLink = styled(MuiLink)`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
`;

type Props = React.ComponentProps<typeof MuiLink>;

export const BannerLink = ({ ...props }: Props) => {
  const report = useReportEvent();
  return (
    <StyledMuiLink
      rel="noopener noreferrer"
      target="_blank"
      {...props}
      onClick={() => {
        report('ANNOUNCEMENT_LINK_CLICK', { link: props.href });
      }}
    />
  );
};
