import React, { FC } from 'react';
import { useConfig } from 'tg.globalContext/helpers';
import { GoogleReCaptchaProvider } from 'react-google-recaptcha-v3';

export const RecaptchaProvider: FC = (props) => {
  const config = useConfig();
  if (!config.recaptchaSiteKey) {
    return <>{props.children}</>;
  }

  return (
    <GoogleReCaptchaProvider
      reCaptchaKey={config.recaptchaSiteKey}
      useRecaptchaNet={true}
    >
      {props.children}
    </GoogleReCaptchaProvider>
  );
};
