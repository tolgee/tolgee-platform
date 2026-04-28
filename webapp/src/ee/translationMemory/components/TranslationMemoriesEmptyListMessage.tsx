import { Button, Card, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import React, { VFC } from 'react';

const StyledCard = styled(Card)`
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  border-radius: 20px;
  background-color: ${({ theme }) =>
    theme.palette.tokens.background.onDefaultGrey};
  padding-top: ${({ theme }) => theme.spacing(8)};
  padding-bottom: ${({ theme }) => theme.spacing(8)};
`;

const StyledText = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: ${({ theme }) => theme.spacing(0, 8, 2, 8)};
`;

const StyledButton = styled(Button)`
  margin-top: ${({ theme }) => theme.spacing(4)};
`;

type Props = {
  loading?: boolean;
  onCreateClick?: () => void;
};

export const TranslationMemoriesEmptyListMessage: VFC<Props> = ({
  loading,
  onCreateClick,
}) => {
  if (loading) {
    return null;
  }

  return (
    <StyledCard elevation={0}>
      <StyledText>
        <Typography variant="h4">
          <T
            keyName="translation_memories_list_empty_title"
            defaultValue="No translation memories yet"
          />
        </Typography>
        <Typography mt={2}>
          <T
            keyName="translation_memories_list_empty_message"
            defaultValue="Create a translation memory to reuse existing translations across projects."
          />
        </Typography>
      </StyledText>
      {onCreateClick && (
        <StyledButton
          data-cy="translation-memories-empty-add-button"
          color="primary"
          variant="contained"
          aria-label="add"
          onClick={onCreateClick}
        >
          <T
            keyName="translation_memories_add_first_button"
            defaultValue="New translation memory"
          />
        </StyledButton>
      )}
    </StyledCard>
  );
};
