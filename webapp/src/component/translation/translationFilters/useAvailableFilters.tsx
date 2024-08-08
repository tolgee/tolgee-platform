import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { StateType, TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { encodeFilter, GroupType } from './tools';
import { LanguageModel } from './tools';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';

export type FilterOptions = {
  keyRelatedOnly: boolean;
};

export const useAvailableFilters = (
  selectedLanguages?: LanguageModel[],
  options?: FilterOptions
) => {
  const project = useProject();
  const { t } = useTranslate();
  const translateState = useStateTranslation();

  const tags = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const namespaces = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
  });

  const availableFilters: GroupType[] = [
    {
      name: null,
      type: 'multi',
      options: [
        {
          label: t('translations_filters_heading_tags'),
          value: null,
          submenu:
            tags.data?._embedded?.tags?.map((val) => {
              return {
                label: val.name,
                value: encodeFilter({
                  filter: 'filterTag',
                  value: val.name,
                }),
              };
            }) || [],
        },
        {
          label: t('translations_filters_heading_namespaces'),
          value: null,
          submenu:
            namespaces.data?._embedded?.namespaces?.map((val) => {
              return {
                label: val.name || t('namespace_default'),
                value: encodeFilter({
                  filter: 'filterNamespace',
                  value: val.name || '',
                }),
              };
            }) || [],
        },
      ],
    },
  ];

  if (!options?.keyRelatedOnly) {
    availableFilters.push(
      {
        name: t('translations_filters_heading_translations'),
        options: [
          {
            label: t('translations_filters_missing_translation'),
            value: encodeFilter({
              filter: 'filterUntranslatedAny',
              value: true,
            }),
          },
        ],
      },
      {
        name: null,
        options: [
          {
            label: t('translations_filters_something_outdated'),
            value: encodeFilter({
              filter: 'filterOutdatedLanguage',
              value: selectedLanguages?.map((l) => l.tag) || [],
            }),
          },
        ],
      }
    );
  }

  availableFilters.push({
    name: t('translations_filters_heading_screenshots'),
    options: [
      {
        label: t('translations_filters_no_screenshots'),
        value: encodeFilter({
          filter: 'filterHasNoScreenshot',
          value: true,
        }),
      },
      {
        label: t('translations_filters_with_screenshots'),
        value: encodeFilter({
          filter: 'filterHasScreenshot',
          value: true,
        }),
      },
    ],
  });

  if (!options?.keyRelatedOnly) {
    availableFilters.push({
      name: t('translations_filters_heading_states'),
      type: 'states',
      options:
        selectedLanguages?.map((language) => {
          return {
            label: language.name,
            value: null,
            submenu: Object.entries(TRANSLATION_STATES)
              // MACHINE_TRANSLATED is not supported yet
              .filter(([key]) => key !== 'MACHINE_TRANSLATED')
              .map(([key, value]) => {
                return {
                  label: translateState(key as StateType),
                  value: encodeFilter({
                    filter: 'filterState',
                    value: `${language.tag},${key}`,
                  }),
                };
              }),
          };
        }) || [],
    });
  }

  return {
    refresh: () => {
      tags.refetch();
      namespaces.refetch();
    },
    availableFilters,
  };
};
