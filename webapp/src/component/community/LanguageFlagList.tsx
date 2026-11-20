import { Box, Tooltip, TooltipProps } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { StyledCircledPill } from 'tg.component/languages/CircledLanguageIconList';
import { CIRCLED_ICON_PADDING } from 'tg.component/languages/CircledLanguageIcon';

const MAX_FLAGS = 5;
const textBaselinePillAlignment = {
  alignSelf: 'flex-start',
  mt: `-${CIRCLED_ICON_PADDING}px`,
};

type Props = {
  languages: { tag: string; name: string; flagEmoji?: string }[];
  flagHeight: number;
  pillWidth: number;
  gap: number;
  flagsOnTextBaseline?: boolean;
  tooltipProps?: Partial<TooltipProps>;
};

export const LanguageFlagList = ({
  languages,
  flagHeight,
  pillWidth,
  gap,
  flagsOnTextBaseline,
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
            display={flagsOnTextBaseline ? 'block' : 'flex'}
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
            sx={flagsOnTextBaseline ? textBaselinePillAlignment : undefined}
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
