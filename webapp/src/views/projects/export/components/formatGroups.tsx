import { components } from 'tg.service/apiSchema.generated';
import { ReactNode } from 'react';
import { T } from '@tolgee/react';

export type MessageFormat = NonNullable<
  components['schemas']['ExportParams']['messageFormat']
>;

export interface FormatItem {
  id: string;
  name: ReactNode;
  defaultStructureDelimiter?: string;
  showSupportArrays?: boolean;
  defaultSupportArrays?: boolean;
  structured?: boolean;
  format: components['schemas']['ExportParams']['format'];
  messageFormat?: MessageFormat;
  matchByExportParams?: (params: ExportParamsWithoutZip) => boolean;
  extension: string;
  supportedMessageFormats?: MessageFormat[];
}

export interface FormatGroup {
  name: ReactNode;
  formats: FormatItem[];
}

export const formatGroups: FormatGroup[] = [
  {
    name: 'Tolgee Native',
    formats: [
      {
        id: 'native_json',
        extension: 'json',
        name: <T keyName="export-format-flat-json" />,
        defaultStructureDelimiter: '',
        structured: false,
        showSupportArrays: false,
        defaultSupportArrays: false,
        format: 'JSON',
        matchByExportParams: (params) =>
          params.format === 'JSON' &&
          (params.structureDelimiter === '' ||
            params.structureDelimiter == null) &&
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
        name: <T keyName="export-format-xliff" />,
        format: 'XLIFF',
        supportedMessageFormats: [
          'ICU',
          'JAVA_STRING_FORMAT',
          'PHP_SPRINTF',
          'C_SPRINTF',
          'RUBY_SPRINTF',
        ],
      },
      {
        id: 'generic_structured_json',
        extension: 'json',
        name: <T keyName="export-format-structured-json" />,
        defaultStructureDelimiter: '.',
        structured: true,
        showSupportArrays: true,
        defaultSupportArrays: true,
        format: 'JSON',
        matchByExportParams: (params) =>
          params.format === 'JSON' && params.structureDelimiter === '.',
        supportedMessageFormats: [
          'ICU',
          'JAVA_STRING_FORMAT',
          'PHP_SPRINTF',
          'C_SPRINTF',
          'RUBY_SPRINTF',
        ],
      },
      {
        id: 'po',
        extension: 'po',
        name: <T keyName="export-format-po" />,
        format: 'PO',
        supportedMessageFormats: [
          'PHP_SPRINTF',
          'C_SPRINTF',
          'JAVA_STRING_FORMAT',
          'ICU',
          'RUBY_SPRINTF',
        ],
      },
      {
        id: 'properties',
        extension: 'properties',
        name: <T keyName="export-format-properties" />,
        format: 'PROPERTIES',
        supportedMessageFormats: ['ICU', 'JAVA_STRING_FORMAT'],
      },
      {
        id: 'generic_flat_yaml',
        extension: 'yaml',
        name: <T keyName="export-format-flat-yaml" />,
        format: 'YAML',
        defaultStructureDelimiter: '',
        structured: false,
        showSupportArrays: false,
        defaultSupportArrays: false,
        matchByExportParams: (params) => {
          return (
            params.format === 'YAML' &&
            (params.structureDelimiter === '' ||
              params.structureDelimiter == null) &&
            !params.supportArrays
          );
        },
        supportedMessageFormats: [
          'ICU',
          'JAVA_STRING_FORMAT',
          'PHP_SPRINTF',
          'C_SPRINTF',
        ],
      },
      {
        id: 'generic_structured_yaml',
        extension: 'yaml',
        name: <T keyName="export-format-structured-yaml" />,
        defaultStructureDelimiter: '.',
        structured: true,
        showSupportArrays: true,
        defaultSupportArrays: true,
        format: 'YAML',
        matchByExportParams: (params) =>
          params.format === 'YAML' && params.structureDelimiter === '.',
        supportedMessageFormats: [
          'ICU',
          'JAVA_STRING_FORMAT',
          'PHP_SPRINTF',
          'C_SPRINTF',
        ],
      },
      {
        id: 'generic_csv',
        extension: 'csv',
        name: <T keyName="export-format-csv" />,
        format: 'CSV',
        supportedMessageFormats: [
          'ICU',
          'JAVA_STRING_FORMAT',
          'PHP_SPRINTF',
          'C_SPRINTF',
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
        name: <T keyName="export-format-apple-strings" />,
        format: 'APPLE_STRINGS_STRINGSDICT',
      },
      {
        id: 'apple_xliff',
        extension: 'xliff',
        name: <T keyName="export-format-apple-xliff" />,
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
        name: <T keyName="export-format-android-xml" />,
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
        name: <T keyName="export-format-flutter-arb" />,
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
        messageFormat: 'RUBY_SPRINTF',
        name: <T keyName="export-format-ruby-yaml" />,
        format: 'YAML_RUBY',
      },
    ],
  },
  {
    name: 'i18next',
    formats: [
      {
        id: 'i18next_flat_json',
        extension: 'json',
        messageFormat: 'I18NEXT',
        defaultStructureDelimiter: '',
        structured: false,
        showSupportArrays: true,
        defaultSupportArrays: true,
        name: <T keyName="export-format-i18next-json" />,
        format: 'JSON_I18NEXT',
        matchByExportParams: (params) =>
          params.format === 'JSON_I18NEXT' &&
          (params.structureDelimiter === '' ||
            params.structureDelimiter == null) &&
          !params.supportArrays,
      },
      {
        id: 'i18next_structured_json',
        extension: 'json',
        messageFormat: 'I18NEXT',
        defaultStructureDelimiter: '.',
        structured: true,
        showSupportArrays: true,
        defaultSupportArrays: true,
        name: <T keyName="export-format-i18next-json-structured" />,
        format: 'JSON_I18NEXT',
        matchByExportParams: (params) =>
          params.format === 'JSON_I18NEXT' && params.structureDelimiter === '.',
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
              (f.messageFormat == params.messageFormat ||
                f.messageFormat == null)
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

export const normalizeSelectedMessageFormat = (params: {
  format: string;
  messageFormat: MessageFormat | undefined;
}) => {
  const supportedFormats = getFormatById(params.format).supportedMessageFormats;
  if (
    supportedFormats &&
    (params.messageFormat == null ||
      !supportedFormats.includes(params.messageFormat))
  ) {
    return supportedFormats[0];
  }
  if (params.messageFormat == null && !supportedFormats) {
    return undefined;
  }
  return params.messageFormat;
};
