import { Box, Tooltip, TooltipProps } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

import { StyledCircledPill } from 'tg.component/languages/CircledLanguageIconList';

const MAX_FLAGS = 5;

type Props = {
  languages: { tag: string; name: string; flagEmoji?: string }[];
  flagHeight: number;
  pillWidth: number;
  gap: number;
  tooltipProps?: Partial<TooltipProps>;
};

export const LanguageFlagList = ({
  languages,
  flagHeight,
  pillWidth,
  gap,
  tooltipProps,
}: Props) => {
  const shownLanguages = languages.slice(0, MAX_FLAGS);
  const hiddenLanguages = languages.slice(MAX_FLAGS);

  return (
    <Box display="flex" alignItems="center" gap={gap}>
      {shownLanguages.map((l) => (
        <Tooltip key={l.tag} title={l.name} {...tooltipProps}>
          <Box display="flex" data-cy="community-language-flag">
            <FlagImage flagEmoji={l.flagEmoji || ''} height={flagHeight} />
          </Box>
        </Tooltip>
      ))}
      {hiddenLanguages.length > 0 && (
        <Tooltip
          title={hiddenLanguages.map((l) => l.name).join(', ')}
          {...tooltipProps}
        >
          <div data-cy="community-language-flag-more">
            <StyledCircledPill
              height={flagHeight}
              width={pillWidth}
              wrapperProps={{ className: 'wrapped' }}
            >
              +{hiddenLanguages.length}
            </StyledCircledPill>
          </div>
        </Tooltip>
      )}
    </Box>
  );
};
