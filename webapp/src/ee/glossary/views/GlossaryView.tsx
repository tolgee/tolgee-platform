import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import {
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import React, { useMemo, useState } from 'react';
import { GlossaryTermCreateUpdateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateUpdateDialog';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { GlossaryEmptyListMessage } from 'tg.ee.module/glossary/components/GlossaryEmptyListMessage';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export const GlossaryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(undefined);

  const organization = useOrganization();
  const match = useRouteMatch();
  const glossaryId = match.params[PARAMS.GLOSSARY_ID];
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const { t } = useTranslate();

  const glossary = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: { organizationId: organization!.id, glossaryId },
  });

  const selectedLanguagesWithBaseLanguage = useMemo(() => {
    if (selectedLanguages === undefined) {
      return undefined;
    }
    return [glossary.data?.baseLanguageTag || '', ...(selectedLanguages ?? [])];
  }, [selectedLanguages, glossary.data]);

  const path = { organizationId: organization!.id, glossaryId };
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
      languages.filter((l) => l !== glossary.data?.baseLanguageTag)
    );
  };

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const canCreate = ['OWNER', 'MAINTAINER'].includes(
    organization?.currentUserRole || ''
  );

  const onFetchNextPage = () => {
    if (!termsLoadable.isFetching && termsLoadable.hasNextPage) {
      termsLoadable.fetchNextPage();
    }
  };

  return (
    <BaseOrganizationSettingsView
      windowTitle={glossary.data?.name || t('organization_glossary_title')}
      link={LINKS.ORGANIZATION_GLOSSARY}
      navigation={[
        [
          t('organization_glossaries_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
          }),
        ],
        [
          !glossary.isLoading &&
            (glossary.data?.name || t('organization_glossary_view_title')),
          glossary.data &&
            LINKS.ORGANIZATION_GLOSSARY.build({
              [PARAMS.GLOSSARY_ID]: glossaryId,
              [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
            }),
        ],
      ]}
      loading={glossary.isLoading || termsLoadable.isLoading}
      hideChildrenOnLoading={false}
      maxWidth="max"
      allCentered={false}
    >
      {canCreate && createDialogOpen && organization !== undefined && (
        <GlossaryTermCreateUpdateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
          organizationId={organization.id}
          glossaryId={glossaryId}
        />
      )}
      {(terms.length > 0 || search.length > 0) && organization !== undefined ? (
        <GlossaryViewBody
          organization={organization}
          glossaryId={glossaryId}
          loading={termsLoadable.isLoading}
          data={terms}
          fetchDataIds={fetchAllTermsIds}
          totalElements={totalTerms}
          baseLanguage={glossary.data?.baseLanguageTag}
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
