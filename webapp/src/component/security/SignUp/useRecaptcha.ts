import { useGoogleReCaptcha } from 'react-google-recaptcha-v3';
import { useCallback } from 'react';
import { useConfig } from 'tg.globalContext/helpers';

export const useRecaptcha = () => {
  const { executeRecaptcha } = useGoogleReCaptcha();

  const remoteConfig = useConfig();

  return useCallback(async () => {
    if (remoteConfig.recaptchaSiteKey) {
      if (!executeRecaptcha) {
        throw Error('Execute recaptcha not yet available');
      }

      return await executeRecaptcha('sign_up');
    }
    return undefined;
  }, [executeRecaptcha, remoteConfig.recaptchaSiteKey]);
};
