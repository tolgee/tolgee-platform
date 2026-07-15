import { vi } from 'vitest';

export const captureDownloadAnchor = () => {
  const revokeObjectURL = vi.fn();
  const anchor = document.createElement('a');
  vi.spyOn(anchor, 'click').mockImplementation(() => {});
  vi.spyOn(document, 'createElement').mockReturnValue(anchor);
  vi.stubGlobal('URL', {
    createObjectURL: vi.fn(() => 'blob:mock'),
    revokeObjectURL,
  });
  return { anchor, revokeObjectURL };
};
