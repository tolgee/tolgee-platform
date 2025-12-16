import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';

const StyledContainer = styled('div')`
  grid-column: 1 / span 3;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  z-index: 1;
`;

const StyledLine = styled(Box)`
  height: 1px;
  background: ${({ theme }) => theme.palette.divider};
  flex-grow: 1;
`;

const StyledDate = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 12px;
  padding: 0px 10px;
  font-size: 14px;
`;

type Props = {
  date: Date;
};

export const ActivityDateSeparator: React.FC<Props> = ({ date }) => {
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
