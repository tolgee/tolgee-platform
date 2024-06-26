import {styled, useTheme} from '@mui/material';
import {useTranslate} from '@tolgee/react';
import {MailIcon, MailIconDark} from 'tg.component/CustomIcons';

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

export const EmailNotVerifiedAnnouncement = () => {
  const { t } = useTranslate();
  const theme = useTheme();
  return (
    <StyledWrappableContent>
      <StyledContent>
          {theme.palette.mode === 'dark' ? <MailIconDark /> : <MailIcon />}
        <div>{t('announcement_verify_email')}</div>
      </StyledContent>
    </StyledWrappableContent>
  );
};
