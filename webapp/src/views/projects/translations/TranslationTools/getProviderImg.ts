export const getProviderImg = (provider: string | undefined) => {
  switch (provider) {
    case 'GOOGLE':
      return '/images/providers/google-translate.svg';
    case 'AWS':
      return '/images/providers/aws-logo.svg';
    default:
      return null;
  }
};
