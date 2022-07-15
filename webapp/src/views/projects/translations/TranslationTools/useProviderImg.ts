import { useTheme } from '@mui/material';

export const useProviderImg = () => {
  const { palette } = useTheme();

  return (provider: string | undefined) => {
    switch (provider) {
      case 'GOOGLE':
        return '/images/providers/google-translate.svg';
      case 'AWS':
        return `/images/providers/aws-logo-${palette.mode}.svg`;
      case 'DEEPL':
        return `/images/providers/deepl-logo-${palette.mode}.svg`;
      case 'AZURE':
        return `/images/providers/azure-cognitive-logo.svg`;
      default:
        return null;
    }
  };
};
