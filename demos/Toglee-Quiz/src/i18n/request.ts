import { getRequestConfig } from 'next-intl/server';

export default getRequestConfig(async ({ locale }) => {
  return {
    // do this to make next-intl not emmit any warnings
    messages: { locale },
  };
});
