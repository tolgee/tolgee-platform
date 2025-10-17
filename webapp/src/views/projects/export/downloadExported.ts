import { FormatItem } from './components/formatGroups';

export const downloadExported = async (
  response: Response,
  languages: string[],
  format: FormatItem,
  projectName: string
) => {
  const data = await response.blob();
  const onlyPossibleLanguageString =
    languages.length === 1 ? `_${languages[0]}` : '';
  const dateStr = '_' + new Date().toISOString().split('T')[0];
  const url = URL.createObjectURL(data);
  try {
    const a = document.createElement('a');
    try {
      a.href = url;
      if (data.type === 'application/zip') {
        a.download = projectName + dateStr + '.zip';
      } else {
        const extension = parseExtension(response) || format.extension;
        a.download =
          projectName + onlyPossibleLanguageString + dateStr + '.' + extension;
      }
      a.click();
    } finally {
      a.remove();
    }
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 7000);
  }
};

const parseExtension = (response: Response) => {
  const contentDisposition = response.headers.get('Content-Disposition');
  if (contentDisposition) {
    extensionRegex.lastIndex = 0; // Reset the regex to the start of the string
    const match = extensionRegex.exec(contentDisposition);
    if (match) {
      return match[1];
    }
  }
  return null;
};

const extensionRegex = /^.*\.(\w+)"$/gm;
