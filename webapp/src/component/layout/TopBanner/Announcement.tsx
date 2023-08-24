import { styled, Link as MuiLink } from '@mui/material';
import { ContentNoticeIcon } from 'tg.component/CustomIcons';

type Props = {
  content: string;
  link?: string;
};

const StyledLink = styled(MuiLink)`
  display: flex;
  gap: 12px;
  color: ${({ theme }) => theme.palette.text.primary};
  font-size: 12px;
  align-items: center;
`;

const StyledContentNoticeIcon = styled(ContentNoticeIcon)`
  color: ${({ theme }) => theme.palette.topBanner.icon};
`;

export const Announcement = ({ content, link }: Props) => {
  return (
    <StyledLink href={link} target="_blank" rel="noopener noreferrer">
      <StyledContentNoticeIcon />
      <div>{content}</div>
    </StyledLink>
  );
};
