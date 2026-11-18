import { Box, Typography, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Props = { searching: boolean };

export const ContributorsEmptyState = ({ searching }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      gap={3}
      py={6}
      data-cy="community-contributors-empty"
    >
      <Typography sx={{ opacity: 0.8 }}>
        {searching
          ? t('community_contributors_search_empty', 'No contributors found')
          : t(
              'community_contributors_empty',
              'No contributions in this period'
            )}
      </Typography>
      <img
        src={
          theme.palette.mode === 'dark'
            ? '/images/communityEmptyMouseDark.svg'
            : '/images/communityEmptyMouseLight.svg'
        }
        alt=""
        draggable={false}
        height={160}
        style={{ opacity: 0.6 }}
      />
    </Box>
  );
};
