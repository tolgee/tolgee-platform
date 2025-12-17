import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

const StyledStickyContainer = styled('div')`
  display: flex;
  align-items: start;
  justify-self: start;
  position: sticky;
  top: 38px;
  background: ${({ theme }) => theme.palette.background.default};
  margin-left: 8px;
  z-index: 1;
  border-radius: 0px 0px 12px 12px;
`;

const StyledDate = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 12px;
  padding: 0px 10px;
  font-size: 14px;
  background: ${({ theme }) => theme.palette.background.default};
`;

type Props = {
  date: Date;
};

export const StickyDateSeparator: React.FC<Props> = ({ date }) => {
  const lang = useCurrentLanguage();
  const isToday = new Date().toLocaleDateString() === date.toLocaleDateString();
  const { t } = useTranslate();

  return (
    <StyledStickyContainer>
      <StyledDate>
        {isToday && `${t('activity_date_today')} `}
        {date.toLocaleDateString(lang)}
      </StyledDate>
    </StyledStickyContainer>
  );
};
