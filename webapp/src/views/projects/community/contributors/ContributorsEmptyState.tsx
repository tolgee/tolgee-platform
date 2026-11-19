import { Box, Typography, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Props = { searching: boolean; noLanguages: boolean };

export const ContributorsEmptyState = ({ searching, noLanguages }: Props) => {
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
        {noLanguages
          ? t(
              'set_at_least_one_language_error',
              'Select at least one language.'
            )
          : searching
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
