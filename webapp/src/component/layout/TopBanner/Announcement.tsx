import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { TadaIcon } from 'tg.component/CustomIcons';
import { BannerLink } from './BannerLink';

type Props = {
  content: React.ReactNode;
  link?: string;
};

const StyledContent = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
`;

const StyledWrappableContent = styled('div')`
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
`;

export const Announcement = ({ content, link }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledWrappableContent>
      <StyledContent>
        <TadaIcon />
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
