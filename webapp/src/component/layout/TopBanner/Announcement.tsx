import { styled, Link as MuiLink } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { TadaIcon } from 'tg.component/CustomIcons';

type Props = {
  content: string;
  link?: string;
};

const StyledContent = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
`;

const StyledMuiLink = styled(MuiLink)`
  color: ${({ theme }) => theme.palette.topBanner.linkText};
  text-decoration: underline;
`;

export const Announcement = ({ content, link }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledContent>
      <TadaIcon />
      <div>{content}</div>
      {link && (
        <StyledMuiLink href={link} rel="noopener noreferrer" target="_blank">
          {t('announcement_general_link_text')}
        </StyledMuiLink>
      )}
    </StyledContent>
  );
};
