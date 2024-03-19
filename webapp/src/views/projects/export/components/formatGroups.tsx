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
  extension: string;
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
        extension: 'json',
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
        extension: 'xliff',
        name: 'XLIFF',
        format: 'XLIFF',
      },
      {
        id: 'generic_structured_json',
        extension: 'json',
        name: 'Structured JSON',
        defaultStructureDelimiter: '.',
        canBeStructured: true,
        showSupportArrays: true,
        defaultSupportArrays: true,
        format: 'JSON',
        matchByExportParams: (params) =>
          params.format === 'JSON' && params.structureDelimiter === '.',
      },
      {
        id: 'properties',
        extension: 'properties',
        name: '.properties',
        format: 'PROPERTIES',
      },
    ],
  },
  {
    name: 'Gettext (.po)',
    formats: [
      {
        id: 'po_php',
        extension: 'po',
        name: 'PHP .po',
        format: 'PO',
        messageFormat: 'PHP_SPRINTF',
      },
      // {
      //   id: 'po_python',
      //   extension: 'po',
      //   name: 'Python .po',
      //   format: 'PO',
      //   messageFormat: 'PYTHON_SPRINTF',
      // },
      {
        id: 'po_c',
        extension: 'po',
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
        extension: 'strings',
        name: 'Apple .strings & .stringsdict',
        format: 'APPLE_STRINGS_STRINGSDICT',
      },
      {
        id: 'apple_xliff',
        extension: 'xliff',
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
        extension: 'xml',
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
        extension: 'arb',
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
            return (
              f.format === params.format &&
              ((f.messageFormat === undefined &&
                params.messageFormat === null) ||
                f.messageFormat === params.messageFormat)
            );
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
