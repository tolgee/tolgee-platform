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
  supportedMessageFormats?: components['schemas']['ExportParams']['messageFormat'][];
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
        supportedMessageFormats: [
          'ICU',
          'JAVA_SPRINTF',
          'PHP_SPRINTF',
          'C_SPRINTF',
          'RUBY_SPRINTF',
        ],
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
        supportedMessageFormats: [
          'ICU',
          'JAVA_SPRINTF',
          'PHP_SPRINTF',
          'C_SPRINTF',
          'RUBY_SPRINTF',
        ],
      },
      {
        id: 'properties',
        extension: 'properties',
        name: '.properties',
        format: 'PROPERTIES',
        supportedMessageFormats: ['ICU', 'JAVA_SPRINTF'],
      },
      {
        id: 'yaml',
        extension: 'properties',
        name: 'YAML',
        format: 'YAML',
        supportedMessageFormats: [
          'ICU',
          'JAVA_SPRINTF',
          'PHP_SPRINTF',
          'C_SPRINTF',
        ],
      },
    ],
  },
  {
    name: 'Gettext (.po)',
    formats: [
      {
        id: 'po',
        extension: 'po',
        name: 'PHP .po',
        format: 'PO',
        supportedMessageFormats: [
          'PHP_SPRINTF',
          'C_SPRINTF',
          'JAVA_SPRINTF',
          'ICU',
          'RUBY_SPRINTF',
        ],
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
  {
    name: 'Ruby on Rails',
    formats: [
      {
        id: 'ruby_yaml',
        extension: 'yaml',
        name: 'Ruby .yaml',
        format: 'YAML_RUBY',
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
