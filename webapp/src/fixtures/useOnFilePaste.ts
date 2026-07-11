import { useEffect } from 'react';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

export const useOnFilePaste = (onFilePaste: (files: FilesType) => void) => {
  useEffect(() => {
    const pasteListener = (e: ClipboardEvent) => {
      const files: File[] = [];
      if (!e.clipboardData?.files.length) {
        return;
      }
      for (let i = 0; i < e.clipboardData.files.length; i++) {
        const item = e.clipboardData.files.item(i);
        if (item) {
          files.push(item);
        }
      }
      onFilePaste(files.map((f) => ({ file: f, name: f.name })));
    };

    document.addEventListener('paste', pasteListener);

    return () => {
      document.removeEventListener('paste', pasteListener);
    };
  }, [onFilePaste]);
};
