import { Box } from '@mui/material';
import { OVERLAP_HINT, UNUSED_HINT } from './iconHints';
import type { IconMeaning } from './iconMeta';

type Props = {
  meaning?: IconMeaning;
  /** Absolute inside a grid tile, inline next to a name in the list. */
  placement: 'tile' | 'inline';
};

const Dot = ({ filled, title }: { filled: boolean; title: string }) => (
  <Box
    component="span"
    title={title}
    sx={{
      width: 7,
      height: 7,
      borderRadius: '50%',
      flex: '0 0 auto',
      background: (theme) =>
        filled ? theme.palette.error.main : 'transparent',
      border: (theme) => `1px solid ${theme.palette.error.main}`,
    }}
  />
);

export const IconFlags = ({ meaning, placement }: Props) => {
  if (!meaning?.prefer && !meaning?.unused) return null;

  return (
    <Box
      sx={
        placement === 'tile'
          ? {
              position: 'absolute',
              top: 3,
              right: 3,
              display: 'flex',
              gap: 0.25,
            }
          : { display: 'inline-flex', gap: 0.5, alignItems: 'center' }
      }
    >
      {meaning.prefer && (
        <Dot filled title={`${OVERLAP_HINT}: prefer ${meaning.prefer}`} />
      )}
      {meaning.unused && <Dot filled={false} title={UNUSED_HINT} />}
    </Box>
  );
};
