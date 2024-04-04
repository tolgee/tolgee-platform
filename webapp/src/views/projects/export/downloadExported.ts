import { FormatItem } from './components/formatGroups';

export const downloadExported = async (
  response: Response,
  languages: string[],
  format: FormatItem,
  projectName: string
) => {
  const res = response as unknown as Response;
  const data = await res.blob();
  const url = URL.createObjectURL(data);
  const a = document.createElement('a');
  const onlyPossibleLanguageString =
    languages.length === 1 ? `_${languages[0]}` : '';
  a.href = url;
  const dateStr = '_' + new Date().toISOString().split('T')[0];
  if (data.type === 'application/zip') {
    a.download = projectName + dateStr + '.zip';
  } else {
    const extension = parseExtension(res) || format.extension;
    a.download =
      projectName + onlyPossibleLanguageString + dateStr + '.' + extension;
  }
  a.click();
};

const parseExtension = (response: Response) => {
  const contentDisposition = response.headers.get('Content-Disposition');
  if (contentDisposition) {
    const match = extensionRegex.exec(contentDisposition);
    if (match) {
      return match[1];
    }
  }
  return null;
};

const extensionRegex = /^.*\.(\w+)"$/gm;
