import { Button, Card, styled, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { T, TranslationKey } from '@tolgee/react';
import React from 'react';

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

const StyledImage = styled(Box)`
  max-width: 100%;
  color: ${({ theme }) => theme.palette.text.secondary};
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

type EmptyMessageTranslation = {
  keyName: TranslationKey;
  defaultValue?: string;
  params?: Record<string, React.ReactNode>;
};

type AddFirstResourceMessageProps = {
  title: EmptyMessageTranslation;
  message: EmptyMessageTranslation;
  button?: EmptyMessageTranslation & {
    dataCy: string;
    onClick?: () => void;
  };
  image?: React.ReactNode;
  imageHeight?: string;
};

export const EmptyResourceMessage: React.VFC<AddFirstResourceMessageProps> = ({
  title,
  message,
  button,
  image,
  imageHeight,
}) => {
  return (
    <StyledCard elevation={0}>
      <StyledText>
        <Typography variant="h4">
          {/* @tolgee-ignore — key is supplied by the caller via the `title` prop */}
          <T {...title} />
        </Typography>
        <Typography mt={2}>
          {/* @tolgee-ignore — key is supplied by the caller via the `message` prop */}
          <T {...message} />
        </Typography>
      </StyledText>

      {image && (
        <StyledImage draggable="false" height={imageHeight || '170px'}>
          {image}
        </StyledImage>
      )}

      {button?.onClick && (
        <StyledButton
          data-cy={button.dataCy}
          color="primary"
          variant="contained"
          aria-label="add"
          onClick={button.onClick}
        >
          {/* @tolgee-ignore — key is supplied by the caller via the `button` prop */}
          <T
            keyName={button.keyName}
            defaultValue={button.defaultValue}
            params={button.params}
          />
        </StyledButton>
      )}
    </StyledCard>
  );
};
