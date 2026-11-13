import React from 'react';
import { Box, Tooltip, Typography } from '@mui/material';

type DataProps = Record<`data-${string}`, string>;

type Props = {
  title: React.ReactNode;
  score?: number;
  // Explains the number on hover (the card says what the threshold needs).
  scoreTooltip?: React.ReactNode;
  titleProps?: DataProps;
  scoreProps?: DataProps;
};

// The first line of an AI review result: the title on the left and the plain score on the right,
// both 16px medium. Used by the AI review card and the review prompt preview.
export const AiReviewScoreTitle = ({
  title,
  score,
  scoreTooltip,
  titleProps,
  scoreProps,
}: Props) => {
  const scoreText = score != null && (
    <Typography
      fontWeight={500}
      sx={{
        fontVariantNumeric: 'tabular-nums',
        cursor: scoreTooltip ? 'default' : undefined,
      }}
      {...scoreProps}
    >
      {score}
    </Typography>
  );

  return (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems="baseline"
      gap={1}
    >
      <Typography fontWeight={500} {...titleProps}>
        {title}
      </Typography>
      {scoreText &&
        (scoreTooltip ? (
          <Tooltip title={scoreTooltip}>{scoreText}</Tooltip>
        ) : (
          scoreText
        ))}
    </Box>
  );
};
