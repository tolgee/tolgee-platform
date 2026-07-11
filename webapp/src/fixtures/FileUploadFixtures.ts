const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

export async function getFilesAsync(dataTransfer: DataTransfer) {
  const files: FilesType = [];

  const items = [...dataTransfer.items].map((item) => ({
    // looks like the dataTransfer is suspect to bugs in browsers, so we need to extract the data from it,
    // or it's pruned when iterating over it
    kind: item.kind,
    webkitEntry:
      typeof item.webkitGetAsEntry === 'function' && !isSafari
        ? item.webkitGetAsEntry()
        : null,
    getAsFile: () => item.getAsFile(),
  }));

  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (item.kind === 'file') {
      // Safari doesn't support the File System Access API and hangs

      if (item.webkitEntry != null) {
        const entry = item.webkitEntry;
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
