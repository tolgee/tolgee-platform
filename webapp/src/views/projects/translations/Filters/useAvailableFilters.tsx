import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { translationStates } from 'tg.constants/translationStates';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { encodeFilter, GroupType } from './tools';

export const useAvailableFilters = (selectedLanguages?: string[]) => {
  const languages = useTranslationsSelector((v) => v.languages);
  const project = useProject();
  const { t } = useTranslate();

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

  return {
    refresh: () => {
      tags.refetch();
      namespaces.refetch();
    },
    availableFilters: [
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
        options: [
          {
            label: t('translations_filters_something_outdated'),
            value: encodeFilter({
              filter: 'filterOutdatedLanguage',
              value: selectedLanguages || [],
            }),
          },
        ],
      },
      {
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
      },
      {
        name: t('translations_filters_heading_states'),
        type: 'states',
        options:
          selectedLanguages?.map((l) => {
            const language =
              languages?.find((lang) => lang.tag === l)?.name || l;
            return {
              label: language,
              value: null,
              submenu: Object.entries(translationStates)
                // MACHINE_TRANSLATED is not supported yet
                .filter(([key]) => key !== 'MACHINE_TRANSLATED')
                .map(([key, value]) => {
                  return {
                    label: t(value.translationKey),
                    value: encodeFilter({
                      filter: 'filterState',
                      value: `${l},${key}`,
                    }),
                  };
                }),
            };
          }) || [],
      },
    ] as GroupType[],
  };
};
