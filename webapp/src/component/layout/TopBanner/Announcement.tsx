import { Box, styled, SxProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Tada } from 'tg.component/CustomIcons';
import { BannerLink } from './BannerLink';
import React from 'react';

type Props = {
  content: React.ReactNode;
  link?: string;
  icon?: React.ReactNode;
  title?: React.ReactNode;
  action?: React.ReactNode;
  sx?: SxProps;
  className?: string;
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
`;

const StyledWrappableContent = styled(Box)`
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
`;

export const Announcement = ({
  content,
  link,
  icon,
  title,
  action,
  sx,
  className,
}: Props) => {
  const { t } = useTranslate();

  return (
    <StyledWrappableContent {...{ sx, className }}>
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
      {action}
    </StyledWrappableContent>
  );
};
