const filenameRegex = /filename="?([^";]+)"?/i;

export const parseContentDispositionFilename = (
  response: Response
): string | null => {
  const contentDisposition = response.headers.get('Content-Disposition');
  if (!contentDisposition) {
    return null;
  }
  const match = filenameRegex.exec(contentDisposition);
  return match ? match[1] : null;
};

export const sanitizeFilename = (name: string) =>
  name.replace(/[/\\?%*:|"<>]/g, '_').trim() || 'download';

/**
 * Saves a streamed response (e.g. an `application/zip` attachment) to a file,
 * preferring the server's Content-Disposition filename and falling back to
 * `fallbackName`.
 */
export const downloadResponseAsFile = async (
  response: Response,
  fallbackName: string
) => {
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  try {
    const a = document.createElement('a');
    try {
      a.href = url;
      a.download = sanitizeFilename(
        parseContentDispositionFilename(response) ?? fallbackName
      );
      a.click();
    } finally {
      a.remove();
    }
  } finally {
    // Revoking the object URL before the browser has started reading it cancels
    // the in-progress download in some browsers; defer the revoke past that.
    setTimeout(() => URL.revokeObjectURL(url), 7000);
  }
};
