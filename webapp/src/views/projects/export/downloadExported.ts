import { downloadBlobAsFile } from 'tg.fixtures/downloadResponseAsFile';

import { FormatItem } from './components/formatGroups';

export const downloadExported = async (
  response: Response,
  languages: string[],
  format: FormatItem,
  projectName: string,
  branchName?: string
) => {
  const data = await response.blob();
  const onlyPossibleLanguageString =
    languages.length === 1 ? `_${languages[0]}` : '';
  const branchStr = branchName ? `(${branchName})` : '';
  const dateStr = '_' + new Date().toISOString().split('T')[0];
  let filename: string;
  if (data.type === 'application/zip') {
    filename = `${projectName}${branchStr}${dateStr}.zip`;
  } else {
    const extension = parseExtension(response) || format.extension;
    filename = `${projectName}${branchStr}${onlyPossibleLanguageString}${dateStr}.${extension}`;
  }
  downloadBlobAsFile(data, filename);
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
