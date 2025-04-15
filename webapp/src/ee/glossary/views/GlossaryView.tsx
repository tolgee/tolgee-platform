import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import React, { useMemo, useState } from 'react';
import { GlossaryTermCreateUpdateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateUpdateDialog';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { GlossaryEmptyListMessage } from 'tg.ee.module/glossary/components/GlossaryEmptyListMessage';

export const GlossaryView = () => {
  const [search, setSearch] = useState('');
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
    return [
      glossary.data?.baseLanguageCode || '',
      ...(selectedLanguages ?? []),
    ];
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
      languages.filter((l) => l !== glossary.data?.baseLanguageCode)
    );
  };

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

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
    >
      {createDialogOpen && organization !== undefined && (
        <GlossaryTermCreateUpdateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
          organizationId={organization.id}
          glossaryId={glossaryId}
        />
      )}
      {terms.length > 0 && organization !== undefined ? (
        <GlossaryViewBody
          organizationId={organization.id}
          glossaryId={glossaryId}
          data={terms}
          totalElements={totalTerms}
          baseLanguage={glossary.data?.baseLanguageCode}
          selectedLanguages={selectedLanguages}
          selectedLanguagesWithBaseLanguage={selectedLanguagesWithBaseLanguage}
          updateSelectedLanguages={updateSelectedLanguages}
          onFetchNextPage={onFetchNextPage}
          onCreate={onCreate}
          onSearch={setSearch}
        />
      ) : (
        <GlossaryEmptyListMessage
          loading={termsLoadable.isLoading}
          onCreate={onCreate}
          onImport={undefined /* TODO */}
        />
      )}
    </BaseOrganizationSettingsView>
  );
};
