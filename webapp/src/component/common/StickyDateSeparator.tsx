import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';

const StyledContainer = styled('div')`
  display: flex;
  align-items: start;
  position: sticky;
  top: 0px;
  height: 25px;
  background: ${({ theme }) => theme.palette.cellSelected2.main};
  padding-bottom: 1px;
  padding-top: 1px;
  z-index: 1;
  & > * {
    margin-top: -2px;
  }
`;

const StyledLine = styled(Box)`
  height: 1px;
  background: ${({ theme }) => theme.palette.divider};
  flex-grow: 1;
`;

const StyledDate = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 0px 0px 12px 12px;
  padding: 0px 10px;
  font-size: 14px;
`;

type Props = {
  date: Date;
};

export const StickyDateSeparator: React.FC<Props> = ({ date }) => {
  const lang = useCurrentLanguage();
  const isToday = new Date().toLocaleDateString() === date.toLocaleDateString();
  const { t } = useTranslate();

  return (
    <StyledContainer>
      <StyledLine maxWidth={10} />
      <StyledDate>
        {isToday && `${t('activity_date_today')} `}
        {date.toLocaleDateString(lang)}
      </StyledDate>
      <StyledLine />
    </StyledContainer>
  );
};
