import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Tada } from 'tg.component/CustomIcons';
import { BannerLink } from './BannerLink';

type Props = {
  content: React.ReactNode;
  link?: string;
  icon?: React.ReactNode;
  title?: string;
};

const StyledContent = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
`;

const StyledTitle = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
  color: ${({ theme }) =>
    theme.palette.tokens._components.noticeBar.importantColor};
`;

const StyledWrappableContent = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
`;

export const Announcement = ({ content, link, icon, title }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledWrappableContent>
      <StyledContent>
        {icon ? icon : <Tada />}
        {title && <StyledTitle>{title}</StyledTitle>}
        <div>{content}</div>
      </StyledContent>
      {link && (
        <BannerLink href={link}>
          {t('announcement_general_link_text')}
        </BannerLink>
      )}
    </StyledWrappableContent>
  );
};
