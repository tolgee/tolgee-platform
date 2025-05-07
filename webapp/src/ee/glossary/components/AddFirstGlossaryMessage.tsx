import { Button, Card, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import Box from '@mui/material/Box';
import EmptyImage from 'tg.svgs/icons/glossary-empty.svg?react';
import React from 'react';

const StyledCard = styled(Card)`
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  border-radius: 20px;
  background-color: ${({ theme }) =>
    theme.palette.tokens.background.onDefaultGrey};
`;

const StyledImage = styled(Box)`
  max-width: 100%;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledText = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: ${({ theme }) => theme.spacing(8, 8, 2, 8)};
`;

const StyledButton = styled(Button)`
  margin-top: ${({ theme }) => theme.spacing(4)};
  margin-bottom: ${({ theme }) => theme.spacing(8)};
`;

export type AddFirstGlossaryMessageProps = {
  height?: string;
  onCreateClick?: () => void;
};

export const AddFirstGlossaryMessage: React.VFC<
  AddFirstGlossaryMessageProps
> = (props) => {
  return (
    <StyledCard elevation={0}>
      <StyledText>
        <Typography variant="h4">
          <T keyName="glossaries_list_empty_title" />
        </Typography>
        <Typography mt={2}>
          <T
            keyName="glossaries_list_empty_message"
            params={{
              bestPractice: (
                <a href="https://docs.tolgee.io/platform/projects_and_organizations/glossary" />
              ),
            }}
          />
        </Typography>
      </StyledText>
      <StyledImage draggable="false" height={props.height || '170px'}>
        <EmptyImage />
      </StyledImage>
      {props.onCreateClick && (
        <StyledButton
          data-cy="glossaries-empty-add-button"
          color="primary"
          variant="contained"
          aria-label="add"
          onClick={props.onCreateClick}
        >
          <T keyName="glossaries_add_first_button" />
        </StyledButton>
      )}
    </StyledCard>
  );
};
