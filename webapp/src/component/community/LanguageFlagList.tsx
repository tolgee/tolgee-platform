import { Box, Tooltip, TooltipProps } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { StyledCircledPill } from 'tg.component/languages/CircledLanguageIconList';

const MAX_FLAGS = 5;
// -2px cancels CircledPill's 2px padding so the pill's top meets the inline flags' top
const inlinePillAlignment = { alignSelf: 'flex-start', mt: '-2px' };

type Props = {
  languages: { tag: string; name: string; flagEmoji?: string }[];
  flagHeight: number;
  pillWidth: number;
  gap: number;
  inlineFlags?: boolean;
  tooltipProps?: Partial<TooltipProps>;
};

export const LanguageFlagList = ({
  languages,
  flagHeight,
  pillWidth,
  gap,
  inlineFlags,
  tooltipProps,
}: Props) => {
  const shownLanguages = languages.slice(0, MAX_FLAGS);
  const hiddenLanguages = languages.slice(MAX_FLAGS);

  return (
    <Box display="flex" alignItems="center" gap={gap}>
      {shownLanguages.map((l) => (
        <Tooltip key={l.tag} title={l.name} {...tooltipProps}>
          <Box
            component="span"
            display={inlineFlags ? undefined : 'flex'}
            data-cy="community-language-flag"
          >
            <FlagImage flagEmoji={l.flagEmoji || ''} height={flagHeight} />
          </Box>
        </Tooltip>
      ))}
      {hiddenLanguages.length > 0 && (
        <Tooltip
          title={hiddenLanguages.map((l) => l.name).join(', ')}
          {...tooltipProps}
        >
          <Box
            data-cy="community-language-flag-more"
            sx={inlineFlags ? inlinePillAlignment : undefined}
          >
            <StyledCircledPill
              height={flagHeight}
              width={pillWidth}
              wrapperProps={{ className: 'wrapped' }}
            >
              +{hiddenLanguages.length}
            </StyledCircledPill>
          </Box>
        </Tooltip>
      )}
    </Box>
  );
};
