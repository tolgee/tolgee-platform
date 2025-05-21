import { useTheme } from '@mui/material';

export const useServiceImg = () => {
  const { palette } = useTheme();

  return (service: string | undefined, contextPresent: boolean | undefined) => {
    switch (service) {
      case 'GOOGLE':
        return '/images/providers/google-translate.svg';
      case 'AWS':
        return `/images/providers/aws-logo-${palette.mode}.svg`;
      case 'DEEPL':
        return `/images/providers/deepl-logo-${palette.mode}.svg`;
      case 'AZURE':
        return `/images/providers/azure-cognitive-logo.svg`;
      case 'BAIDU':
        return `/images/providers/baidu-icon.svg`;
      case 'PROMPT':
        return contextPresent
          ? `/images/providers/tolgee-logo-${palette.mode}-in-context.svg`
          : `/images/providers/tolgee-logo-${palette.mode}.svg`;
      default:
        return undefined;
    }
  };
};
