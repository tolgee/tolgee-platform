import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import React, { useMemo, useState } from 'react';
import { GlossaryTermCreateUpdateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateUpdateDialog';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { GlossaryEmptyListMessage } from 'tg.ee.module/glossary/components/GlossaryEmptyListMessage';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

export const GlossaryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(undefined);

  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const { t } = useTranslate();

  const selectedLanguagesWithBaseLanguage = useMemo(() => {
    if (selectedLanguages === undefined) {
      return undefined;
    }
    return [glossary?.baseLanguageTag || '', ...(selectedLanguages ?? [])];
  }, [selectedLanguages, glossary]);

  const path = {
    organizationId: preferredOrganization!.id,
    glossaryId: glossary.id,
  };
  const query = {
    search: search,
    languageTags: selectedLanguagesWithBaseLanguage,
    size: 30,
    sort: ['id,desc'],
  };
  const termsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/termsWithTranslations',
    method: 'get',
    path: path,
    query: query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: path,
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const getTermsIdsMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/termsIds',
    method: 'get',
  });

  const fetchAllTermsIds = () => {
    return new Promise<number[]>((resolve, reject) => {
      getTermsIdsMutation.mutate(
        {
          path,
          query,
        },
        {
          onSuccess: (data) => {
            resolve(data._embedded?.longList ?? []);
          },
          onError: (e) => {
            reject(e);
          },
        }
      );
    });
  };

  const terms = useMemo(
    () =>
      termsLoadable.data?.pages.flatMap(
        (p) => p._embedded?.glossaryTerms ?? []
      ) ?? [],
    [termsLoadable.data]
  );

  const totalTerms = termsLoadable.data?.pages?.[0]?.page?.totalElements;

  const updateSelectedLanguages = (languages: string[]) => {
    setSelectedLanguages(
      languages.filter((l) => l !== glossary.baseLanguageTag)
    );
  };

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const canCreate = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

  const onFetchNextPage = () => {
    if (!termsLoadable.isFetching && termsLoadable.hasNextPage) {
      termsLoadable.fetchNextPage();
    }
  };

  return (
    <BaseOrganizationSettingsView
      windowTitle={glossary.name || t('organization_glossary_title')}
      link={LINKS.ORGANIZATION_GLOSSARY}
      navigation={[
        [
          t('organization_glossaries_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
        [
          glossary.name || t('organization_glossary_view_title'),
          LINKS.ORGANIZATION_GLOSSARY.build({
            [PARAMS.GLOSSARY_ID]: glossary.id,
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
      ]}
      loading={termsLoadable.isLoading}
      hideChildrenOnLoading={false}
      maxWidth="max"
      allCentered={false}
    >
      {canCreate && createDialogOpen && preferredOrganization !== undefined && (
        <GlossaryTermCreateUpdateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
        />
      )}
      {(terms.length > 0 || search.length > 0) &&
      preferredOrganization !== undefined ? (
        <GlossaryViewBody
          loading={termsLoadable.isLoading}
          data={terms}
          fetchDataIds={fetchAllTermsIds}
          totalElements={totalTerms}
          baseLanguage={glossary.baseLanguageTag}
          selectedLanguages={selectedLanguages}
          selectedLanguagesWithBaseLanguage={selectedLanguagesWithBaseLanguage}
          updateSelectedLanguages={updateSelectedLanguages}
          onFetchNextPage={onFetchNextPage}
          onCreate={canCreate ? onCreate : undefined}
          onSearch={setSearch}
          search={search}
        />
      ) : (
        <GlossaryEmptyListMessage
          loading={termsLoadable.isLoading}
          onCreate={canCreate ? onCreate : undefined}
          onImport={undefined /* TODO */}
        />
      )}
    </BaseOrganizationSettingsView>
  );
};
