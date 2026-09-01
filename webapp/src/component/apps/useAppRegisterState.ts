import { useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';

export type AppManifestPreviewModel =
  components['schemas']['AppManifestPreviewModel'];

/**
 * The manifest URL → consent → register progression, shared by every screen that
 * registers an app. It holds no strings, so each screen keeps its own translation
 * keys while the steps behave identically.
 */
export const useAppRegisterState = (
  onClose: () => void,
  initialManifestUrl = ''
) => {
  const [manifestUrl, setManifestUrl] = useState(initialManifestUrl);
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);

  return {
    manifestUrl,
    setManifestUrl,
    preview,
    setPreview,
    step: preview ? ('consent' as const) : ('url' as const),
    back: () => setPreview(null),
    close: () => {
      setManifestUrl('');
      setPreview(null);
      onClose();
    },
  };
};
