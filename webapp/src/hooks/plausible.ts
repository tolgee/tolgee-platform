import { useEffect } from 'react';
import { useConfig } from 'tg.globalContext/helpers';

export const usePlausible = () => {
  const publicConfig = useConfig();

  useEffect(() => {
    if (publicConfig.plausible.domain) {
      const removeUrlScript = appendPlausibleUrlScript(publicConfig.plausible);
      const removeInlineScript = appendPlausibleInlineScript();
      return () => {
        removeUrlScript();
        removeInlineScript();
      };
    }
  }, []);
};

const appendPlausibleUrlScript = (config: {
  domain?: string;
  url: string;
  scriptUrl: string;
}) => {
  return appendScript(config.scriptUrl, false, { domain: config.domain });
};

const appendPlausibleInlineScript = () => {
  const innerHTML = `
  window.plausible = window.plausible || function() { (window.plausible.q = window.plausible.q || []).push(arguments) }
  `;

  return appendScript(undefined, false, {}, innerHTML);
};

const appendScript = (
  url?: string,
  defer = true,
  dataset: DOMStringMap = {},
  innerHTML?: string
) => {
  const script = document.createElement('script');
  if (url) {
    script.src = url;
  }
  Object.entries(dataset).forEach(([key, value]) => {
    script.dataset[key] = value;
  });
  script.defer = defer;
  script.async = true;
  if (innerHTML) {
    script.innerHTML = innerHTML;
  }
  document.head.append(script);
  return () => {
    document.head.removeChild(script);
  };
};
