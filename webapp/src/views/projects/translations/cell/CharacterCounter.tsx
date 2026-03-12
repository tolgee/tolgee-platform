import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

const StyledContainer = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
`;

const StyledWarning = styled('span')`
  color: ${({ theme }) => theme.palette.error.main};
`;

const StyledCount = styled('span')<{ overLimit: boolean }>`
  white-space: nowrap;
  margin-left: auto;
  color: ${({ theme, overLimit }) =>
    overLimit ? theme.palette.error.main : theme.palette.text.secondary};
`;

type Props = {
  currentCount: number;
  maxLimit: number | null | undefined;
};

export const CharacterCounter: React.FC<Props> = ({
  currentCount,
  maxLimit,
}) => {
  const { t } = useTranslate();

  if (maxLimit == null || maxLimit <= 0) {
    return null;
  }

  const overLimit = currentCount > maxLimit;

  return (
    <StyledContainer>
      {overLimit && (
        <StyledWarning>
          {t('translation_char_counter_over_limit', {
            count: String(currentCount - maxLimit),
          })}
        </StyledWarning>
      )}
      <StyledCount overLimit={overLimit}>
        {currentCount}/{maxLimit}
      </StyledCount>
    </StyledContainer>
  );
};
