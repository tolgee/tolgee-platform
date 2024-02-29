import { components } from 'tg.service/apiSchema.generated';

export interface FormatItem {
  id: string;
  name: string;
  defaultStructureDelimiter?: string;
  showSupportArrays?: boolean;
  defaultSupportArrays?: boolean;
  canBeStructured?: boolean;
  format: components['schemas']['ExportParams']['format'];
  messageFormat?: components['schemas']['ExportParams']['messageFormat'];
  matchByExportParams?: (params: ExportParamsWithoutZip) => boolean;
}

export interface FormatGroup {
  name: string;
  formats: FormatItem[];
}

export const formatGroups: FormatGroup[] = [
  {
    name: 'Tolgee Native',
    formats: [
      {
        id: 'native_json',
        name: 'JSON',
        defaultStructureDelimiter: '',
        canBeStructured: false,
        showSupportArrays: false,
        defaultSupportArrays: false,
        format: 'JSON',
        matchByExportParams: (params) =>
          params.format === 'JSON' &&
          params.structureDelimiter === '' &&
          !params.supportArrays,
      },
    ],
  },
  {
    name: 'Generic',
    formats: [
      {
        id: 'generic_xliff',
        name: 'XLIFF',
        format: 'XLIFF',
      },
      {
        id: 'generic_structured_json',
        name: 'Structured JSON',
        defaultStructureDelimiter: '.',
        canBeStructured: true,
        showSupportArrays: true,
        defaultSupportArrays: true,
        format: 'JSON',
        matchByExportParams: (params) =>
          params.format === 'JSON' && params.structureDelimiter === '.',
      },
    ],
  },
  {
    name: 'Gettext (.po)',
    formats: [
      {
        id: 'po_php',
        name: 'PHP .po',
        format: 'PO',
        messageFormat: 'PHP_SPRINTF',
      },
      {
        id: 'po_python',
        name: 'Python .po',
        format: 'PO',
        messageFormat: 'PYTHON_SPRINTF',
      },
      {
        id: 'po_c',
        name: 'C/C++ .po',
        format: 'PO',
        messageFormat: 'C_SPRINTF',
      },
    ],
  },
  {
    name: 'Apple',
    formats: [
      {
        id: 'apple_strings',
        name: 'Apple .strings & .stringsdict',
        format: 'APPLE_STRINGS_STRINGSDICT',
      },
      {
        id: 'apple_xliff',
        name: 'Apple .xliff',
        format: 'APPLE_XLIFF',
      },
    ],
  },
  {
    name: 'Android',
    formats: [
      {
        id: 'android_xml',
        name: 'Android .xml',
        format: 'ANDROID_XML',
      },
    ],
  },
  {
    name: 'Flutter',
    formats: [
      {
        id: 'flutter_arb',
        name: 'Flutter .arb',
        format: 'FLUTTER_ARB',
      },
    ],
  },
];

type ExportParamsWithoutZip = Omit<
  components['schemas']['ExportParams'],
  'zip'
>;

export const findByExportParams = (params: ExportParamsWithoutZip) => {
  return (
    formatGroups
      .map((g) =>
        g.formats.find((f) => {
          if (f.matchByExportParams === undefined) {
            return f.format === params.format;
          }
          return f.matchByExportParams(params);
        })
      )
      .find((f) => f) || formatGroups[0].formats[0]
  );
};

export const getFormatById = (id: string): FormatItem => {
  for (const group of formatGroups) {
    for (const format of group.formats) {
      if (format.id === id) {
        return format;
      }
    }
  }
  return formatGroups[0].formats[0];
};
