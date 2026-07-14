import { FC } from 'react';
import { LanguageIconWithTooltip } from '../../../languages/LanguageIconWithTooltip';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { Box } from '@mui/material';

type ActivityGroupMentionedLanguagesProps = {
  mentionedLanguageIds: number[];
};

export const ActivityGroupMentionedLanguages: FC<
  ActivityGroupMentionedLanguagesProps
> = (props) => {
  const languages = useProjectLanguages();

  //filter only mentioned
  const used = languages.filter((l) =>
    props.mentionedLanguageIds.includes(l.id)
  );

  return (
    <Box display="inline-flex">
      {used.map((l, i) => (
        <LanguageIconWithTooltip key={l.tag} l={l} />
      ))}
    </Box>
  );
};
