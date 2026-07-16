import { Box, styled } from '@mui/material';
import { Users01 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

const StyledItem = styled('div')`
  display: grid;
  grid-auto-flow: column;
  gap: ${({ theme }) => theme.spacing(0.75)};
  align-items: center;
`;

export const CommunityTranslationItem = () => {
  const { t } = useTranslate();
  return (
    <StyledItem data-cy="community-translation-item">
      <Box display="flex">
        <Users01 width={24} height={24} />
      </Box>
      <Box>{t('community_translation_switch_item')}</Box>
    </StyledItem>
  );
};
