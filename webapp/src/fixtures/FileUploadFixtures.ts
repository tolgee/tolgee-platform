export class FileUploadFixtures {
  static dataTransferItemsToArray = (items: DataTransferItemList): File[] => {
    const result = [] as any[];
    for (let i = 0; i < items.length; i++) {
      if (items[i].kind === 'file') {
        result.push(items[i].getAsFile());
      }
    }
    return result;
  };
}

export async function getFilesAsync(dataTransfer: DataTransfer) {
  const files: FilesType = [];
  for (let i = 0; i < dataTransfer.items.length; i++) {
    const item = dataTransfer.items[i];
    if (item.kind === 'file') {
      if (typeof item.webkitGetAsEntry === 'function') {
        const entry = item.webkitGetAsEntry();
        if (!entry) {
          continue;
        }
        const entryContent = await readEntryContentAsync(entry);
        files.push(...entryContent);
        continue;
      }

      const file = item.getAsFile();
      if (file) {
        files.push({ file, name: file.name });
      }
    }
  }

  return files;
}

export type FilesType = {
  file: File;
  name: string;
}[];

// Returns a promise with all the files of the directory hierarchy
function readEntryContentAsync(entry: FileSystemEntry) {
  return new Promise<FilesType>((resolve, reject) => {
    let reading = 0;
    const contents: FilesType = [];

    readEntry([], entry);

    function readEntry(path: string[], entry: FileSystemEntry) {
      if (isFile(entry)) {
        reading++;
        entry.file((file) => {
          reading--;
          const name = (path.join('/') + '/' + file.name).replace(/^\//, '');
          contents.push({ name, file });

          if (reading === 0) {
            resolve(contents);
          }
        });
      } else if (isDirectory(entry)) {
        readReaderContent([...path, entry.name], entry.createReader());
      }
    }

    function readReaderContent(
      path: string[],
      reader: FileSystemDirectoryReader
    ) {
      reading++;

      reader.readEntries(function (entries) {
        reading--;
        for (const entry of entries) {
          readEntry(path, entry);
        }

        if (reading === 0) {
          resolve(contents);
        }
      });
    }
  });
}

// for TypeScript typing (type guard function)
// https://www.typescriptlang.org/docs/handbook/advanced-types.html#user-defined-type-guards
function isDirectory(
  entry: FileSystemEntry
): entry is FileSystemDirectoryEntry {
  return entry.isDirectory;
}

function isFile(entry: FileSystemEntry): entry is FileSystemFileEntry {
  return entry.isFile;
}
