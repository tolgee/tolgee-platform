import { Box, Typography, useTheme } from '@mui/material';

type Props = { message: string };

export const ContributorsEmptyState = ({ message }: Props) => {
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
      <Typography sx={{ opacity: 0.8 }}>{message}</Typography>
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
