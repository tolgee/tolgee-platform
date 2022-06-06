import { styled } from '@mui/material';
import { useCurrentLanguage, T } from '@tolgee/react';

const StyledWrapper = styled('div')`
  grid-area: period;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  date: number | undefined;
};

export const PeriodEnd: React.FC<Props> = ({ date }) => {
  const getCurrentLang = useCurrentLanguage();
  const formatedDate = date
    ? new Date(date).toLocaleDateString(getCurrentLang())
    : '-';

  return (
    <StyledWrapper>
      <div>
        <T keyName="billing_period_end" />
      </div>
      <div>{formatedDate}</div>
    </StyledWrapper>
  );
};
