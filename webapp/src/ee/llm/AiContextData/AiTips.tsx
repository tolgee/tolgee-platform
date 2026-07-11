import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Stars } from 'tg.component/CustomIcons';

const StyledWrapper = styled(Box)`
  border-radius: 4px;
  padding: 16px 20px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  background: ${({ theme }) => theme.palette.tipsBanner.background};
`;

const StyledLabel = styled(Box)`
  display: flex;
  gap: 8px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledItems = styled(Box)`
  display: grid;
`;

type Props = {
  tips: string[];
};

export const AiTips = ({ tips }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledWrapper>
      <Box display="flex" gap={1} py="8px">
        <StyledLabel>
          <Stars />
          <Box>{t('ai_tips_label')}</Box>
        </StyledLabel>
        <StyledItems>
          {tips.map((tip, i) => (
            <Box sx={{ fontStyle: 'italic' }} key={i}>
              {tip}
            </Box>
          ))}
        </StyledItems>
      </Box>
    </StyledWrapper>
  );
};
