import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { T, useTranslate } from '@tolgee/react';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import React, { useMemo, useRef, useState } from 'react';
import { Box, styled } from '@mui/material';
import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';
import { ReactList } from 'tg.component/reactList/ReactList';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import { GlossaryViewLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryViewLanguageSelect';
import { GlossaryViewListHeader } from 'tg.ee.module/glossary/components/GlossaryViewListHeader';
import { GlossaryViewListRow } from 'tg.ee.module/glossary/components/GlossaryViewListRow';

const StyleTermsCount = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-top: ${({ theme }) => theme.spacing(1)};
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

const StyledVerticalScroll = styled('div')`
  overflow-x: scroll;
  overflow-y: hidden;
  scroll-behavior: smooth;
`;

const StyledContent = styled('div')`
  position: relative;
`;

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
`;

export const GlossaryView = () => {
  const [search, setSearch] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(undefined);
  const [selectedTerms, setSelectedTerms] = useState<number[]>([]);
  const [selectedTermsInverted, setSelectedTermsInverted] = useState(false);

  const verticalScrollRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);

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
    // sort: ['id,desc'],
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

  const someTermsSelected =
    selectedTerms.length > 0 && selectedTerms.length < terms.length;
  const allTermsSelected = selectedTermsInverted
    ? selectedTerms.length === 0
    : selectedTerms.length === terms.length;

  const toggleSelectAllTerms = () => {
    setSelectedTerms([]);
    if (selectedTermsInverted && selectedTerms.length === terms.length) {
      return;
    }
    setSelectedTermsInverted(!selectedTermsInverted);
  };
  const toggleSelectedTerm = (termId: number) => {
    if (selectedTerms.includes(termId)) {
      setSelectedTerms(selectedTerms.filter((id) => id !== termId));
    } else {
      setSelectedTerms([...selectedTerms, termId]);
    }
  };

  const updateSelectedLanguages = (languages: string[]) => {
    setSelectedLanguages(
      languages.filter((l) => l !== glossary.data?.baseLanguageCode)
    );
  };

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const body =
    terms.length > 0 ? (
      <>
        {terms && (
          <Box>
            <StyledContainerInner>
              <Box display="flex" justifyContent="space-between">
                <Box display="flex" alignItems="center" gap="8px">
                  <Box>
                    <SecondaryBarSearchField
                      onSearch={setSearch}
                      placeholder={t('glossary_search_placeholder')}
                    />
                  </Box>
                </Box>
                <Box display="flex" gap={2}>
                  <GlossaryViewLanguageSelect
                    organizationId={organization!.id}
                    glossaryId={glossaryId}
                    value={selectedLanguagesWithBaseLanguage}
                    onValueChange={updateSelectedLanguages}
                    sx={{
                      width: '250px',
                    }}
                  />
                  <BaseViewAddButton
                    onClick={onCreate}
                    label={t('glossary_add_button')}
                  />
                </Box>
              </Box>
            </StyledContainerInner>
          </Box>
        )}

        <StyledVerticalScroll ref={verticalScrollRef}>
          <StyledContent>
            <StyleTermsCount>
              <T
                keyName="glossary_view_terms_count"
                params={{
                  count:
                    termsLoadable.data?.pages?.[0]?.page?.totalElements ?? 0,
                }}
              />
            </StyleTermsCount>

            <GlossaryViewListHeader
              selectedLanguages={selectedLanguages}
              allTermsSelected={allTermsSelected}
              someTermsSelected={someTermsSelected}
              onToggleSelectAll={toggleSelectAllTerms}
            />

            <ReactList
              ref={reactListRef}
              threshold={800}
              type="variable"
              itemSizeEstimator={(index, cache) => {
                return cache[index] || 84;
              }}
              // @ts-ignore
              scrollParentGetter={() => window}
              length={terms.length}
              useTranslate3d
              itemRenderer={(index) => {
                const row = terms[index];
                const isLast = index === terms.length - 1;
                if (
                  isLast &&
                  !termsLoadable.isFetching &&
                  termsLoadable.hasNextPage
                ) {
                  termsLoadable.fetchNextPage();
                }

                return (
                  <GlossaryViewListRow
                    key={row.id}
                    item={row}
                    baseLanguage={glossary.data?.baseLanguageCode}
                    selectedLanguages={selectedLanguages}
                    selectedTerms={selectedTerms}
                    selectedTermsInverted={selectedTermsInverted}
                    onToggleSelectedTerm={toggleSelectedTerm}
                  />
                );
              }}
            />
          </StyledContent>
        </StyledVerticalScroll>
      </>
    ) : (
      <>Empty TODO</>
    );

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
      maxWidth="wide"
      allCentered
    >
      {createDialogOpen && (
        <GlossaryTermCreateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
          organizationId={organization!.id}
          glossaryId={glossaryId}
        />
      )}
      {body}
    </BaseOrganizationSettingsView>
  );
};
