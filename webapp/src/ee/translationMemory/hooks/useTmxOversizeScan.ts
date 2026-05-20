import { useEffect, useState } from 'react';

import {
  scanTmxForOversize,
  TM_ENTRY_TEXT_MAX_LENGTH,
  TMX_SCAN_MAX_FILE_BYTES,
} from 'tg.ee.module/translationMemory/services/scanTmxForOversize';

export type TmxScanState =
  | { kind: 'idle' }
  | { kind: 'scanning' }
  | { kind: 'tooLargeToScan' }
  | { kind: 'scanned'; oversizeSegments: number };

/**
 * Reads the picked TMX file and counts segments that exceed the entry size cap, so the import
 * dialog can warn the user *before* the upload roundtrip. Large files skip the scan to keep the
 * main thread responsive — the backend still enforces the cap and surfaces the count via
 * `TmxImportResult.skipped` after upload.
 */
export function useTmxOversizeScan(file: File | null): TmxScanState {
  const [state, setState] = useState<TmxScanState>({ kind: 'idle' });

  useEffect(() => {
    if (!file) {
      setState({ kind: 'idle' });
      return;
    }
    if (file.size > TMX_SCAN_MAX_FILE_BYTES) {
      setState({ kind: 'tooLargeToScan' });
      return;
    }

    let cancelled = false;
    setState({ kind: 'scanning' });
    file
      .text()
      .then((text) => {
        if (cancelled) return;
        const { oversizeSegments } = scanTmxForOversize(
          text,
          TM_ENTRY_TEXT_MAX_LENGTH
        );
        setState({ kind: 'scanned', oversizeSegments });
      })
      .catch(() => {
        if (cancelled) return;
        // If the file can't be read locally for any reason, fall through to backend-side reporting.
        setState({ kind: 'tooLargeToScan' });
      });

    return () => {
      cancelled = true;
    };
  }, [file]);

  return state;
}
