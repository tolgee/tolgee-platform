import { Box, Tooltip, styled } from '@mui/material';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import {
  CircledLanguageIcon,
  CircledPill,
} from 'tg.component/languages/CircledLanguageIcon';

type ProjectWithStatsModel = components['schemas']['ProjectWithStatsModel'];
const StyledCircledLanguageIcon = styled(CircledLanguageIcon)`
  cursor: default;
`;

const StyledCircledPill = styled(CircledPill)`
  cursor: default;
  & .wrapped {
    background: ${({ theme }) => theme.palette.emphasis[200]};
    font-size: 13px;
  }
`;

export const ProjectLanguages = ({ p }: { p: ProjectWithStatsModel }) => {
  const maxLangs = 10;
  const showNumber = maxLangs < p.languages.length - 2;
  const showingLanguages = showNumber
    ? p.languages.slice(0, maxLangs)
    : p.languages;
  return (
    <>
      {showingLanguages.map((l) => (
        <Tooltip
          key={l.id}
          title={`${l.name} | ${l.originalName}`}
          onClick={stopBubble()}
          disableInteractive
        >
          <Box data-cy="project-list-languages-item">
            <StyledCircledLanguageIcon size={20} flag={l.flagEmoji} />
          </Box>
        </Tooltip>
      ))}
      {showNumber && (
        <Box data-cy="project-list-languages-item" onClick={stopBubble()}>
          <Tooltip
            disableInteractive
            title={
              <Box display="flex" flexWrap="wrap">
                {p.languages.slice(maxLangs).map((l, i) => (
                  <Box key={i} data-cy="project-list-languages-item">
                    <StyledCircledLanguageIcon size={20} flag={l.flagEmoji} />
                  </Box>
                ))}
              </Box>
            }
          >
            <div>
              <StyledCircledPill
                height={20}
                width={44}
                wrapperProps={{
                  className: 'wrapped',
                }}
              >
                +{p.languages.length - maxLangs}
              </StyledCircledPill>
            </div>
          </Tooltip>
        </Box>
      )}
    </>
  );
};
