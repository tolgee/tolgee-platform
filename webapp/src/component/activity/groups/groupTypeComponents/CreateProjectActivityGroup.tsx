import React, { FC } from 'react';
import { Box, styled } from '@mui/material';

import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

const StyledLanguages = styled(Box)`
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  padding: 8px 0;
`;

export const CreateProjectExpandedContent: FC<{
  data: Record<string, any> | undefined;
}> = ({ data }) => {
  const languages = (data?.languages || []) as {
    id: number;
    name: string;
    tag: string;
    flagEmoji: string;
  }[];

  return (
    <Box data-cy="activity-group-create-project-detail">
      <Box>{data?.name}</Box>
      <StyledLanguages>
        {languages.map((language) => (
          <Box key={language.id} display="flex" gap="4px" alignItems="center">
            <CircledLanguageIcon flag={language.flagEmoji} size={18} />
            {language.name}
          </Box>
        ))}
      </StyledLanguages>
    </Box>
  );
};
