import { Box, Button, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { ProviderType } from './types';

type Props = {
  provider: ProviderType;
};

const StyledContainer = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider};
  padding: 16px;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledImage = styled('img')`
  max-width: 400px;
  max-height: 30px;
`;

const StyledDescription = styled('div')``;

export const TranslationProvider = ({ provider }: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();
  return (
    <StyledContainer>
      <Box display="flex" justifyContent="space-between">
        <StyledImage src={provider.logo[theme.palette.mode]} />
        <Button variant="contained" color="primary">
          {t('translation_provider_get_quote')}
        </Button>
      </Box>
      <StyledDescription>{provider.description}</StyledDescription>
      {Boolean(provider.services.length) && (
        <ul>
          {provider.services.map((service, i) => (
            <li key={i}>{service}</li>
          ))}
        </ul>
      )}
    </StyledContainer>
  );
};
