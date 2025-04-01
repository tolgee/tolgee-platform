import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { T, useTranslate } from '@tolgee/react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useRouteMatch } from 'react-router-dom';
import React, { useMemo, useRef, useState } from 'react';
import { styled } from '@mui/material';
import {
  CellLanguage,
  CellLanguageModel,
} from 'tg.views/projects/translations/TranslationsTable/CellLanguage';
import { ColumnResizer } from 'tg.views/projects/translations/ColumnResizer';
import { useColumns } from 'tg.views/projects/translations/useColumns';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';

const StyledVerticalScroll = styled('div')`
  overflow-x: scroll;
  overflow-y: hidden;
  scroll-behavior: smooth;
`;

const StyledContent = styled('div')`
  position: relative;
`;

const StyledHeaderRow = styled('div')`
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  top: 0px;
  margin-bottom: -1px;
  display: grid;
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  overflow: hidden;

  &.termCell {
    padding-left: 13px;
    padding-top: 8px;
  }
`;

export const GlossaryView = () => {
  const [search, setSearch] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(undefined);

  const verticalScrollRef = useRef<HTMLDivElement>(null);

  const organization = useOrganization();
  const match = useRouteMatch();
  const glossaryId = match.params[PARAMS.GLOSSARY_ID];
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const { t } = useTranslate();

  const glossary = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{id}',
    method: 'get',
    path: { organizationId: organization!.id, id: glossaryId },
  });

  const glossaryLanguages = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{id}/languages',
    method: 'get',
    path: { organizationId: organization!.id, id: glossaryId },
    options: {
      onSuccess: (data) => {
        if (selectedLanguages === undefined) {
          setSelectedLanguages(data.filter((l) => !l.base).map((l) => l.tag));
        }
      },
    },
  });

  // const terms = useApiQuery({
  //   url: '/v2/organizations/{organizationId}/glossaries/{id}/terms',
  //   method: 'get',
  //   path: { organizationId: organization!.id, id: glossaryId },
  //   query: {
  //     page,
  //     size: 20,
  //     search,
  //     sort: ['id,desc'],
  //   },
  //   options: {
  //     keepPreviousData: true,
  //   },
  // });

  const items = []; // terms?.data?._embedded?.terms;

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const initialColumnRatios = useMemo(() => {
    return [1, ...(selectedLanguages?.map(() => 1) || [])];
  }, [selectedLanguages]);

  const {
    columnSizes,
    columnSizesPercent,
    startResize,
    resizeColumn,
    addResizer,
  } = useColumns({
    width: verticalScrollRef.current?.clientWidth || 1,
    initialRatios: initialColumnRatios,
    minSize: 350,
  });

  return (
    <BaseOrganizationSettingsView
      windowTitle={glossary.data?.name || t('organization_glossary_title')}
      onSearch={setSearch}
      searchPlaceholder={t('glossary_search_placeholder')}
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
      loading={glossary.isLoading} // || terms.isLoading}
      hideChildrenOnLoading={false}
      maxWidth="wide"
      allCentered
      onAdd={items && onCreate}
      addLabel={t('glossary_add_button')}
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

      <StyledVerticalScroll ref={verticalScrollRef}>
        <StyledContent>
          <StyledHeaderRow
            style={{
              gridTemplateColumns: columnSizesPercent.join(' '),
            }}
          >
            <StyledHeaderCell
              key={0}
              style={{
                width: columnSizesPercent[0],
                height: 39,
              }}
              className="termCell"
            >
              <T keyName="glossary_grid_term_text" />
            </StyledHeaderCell>
            {selectedLanguages?.map((tag, i) => {
              const languageData = languageInfo[tag];
              const language: CellLanguageModel = {
                base: false,
                flagEmoji: languageData?.flags?.[0] || '',
                name: languageData?.englishName || tag,
              };
              return (
                <StyledHeaderCell key={i + 1}>
                  <CellLanguage
                    onResize={() => startResize(i)}
                    language={language}
                  />
                </StyledHeaderCell>
              );
            })}
          </StyledHeaderRow>
          {columnSizes.slice(0, -1).map((w, i) => {
            const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
            return (
              <ColumnResizer
                key={i}
                size={w}
                left={left}
                onResize={(size) => resizeColumn(i, size)}
                passResizeCallback={(callback) => addResizer(i, callback)}
              />
            );
          })}

          {/*<ReactList*/}
          {/*  ref={reactListRef}*/}
          {/*  threshold={800}*/}
          {/*  type="variable"*/}
          {/*  itemSizeEstimator={(index, cache) => {*/}
          {/*    return cache[index] || 84;*/}
          {/*  }}*/}
          {/*  // @ts-ignore*/}
          {/*  scrollParentGetter={() => window}*/}
          {/*  length={translations.length}*/}
          {/*  useTranslate3d*/}
          {/*  itemRenderer={(index) => {*/}
          {/*    const row = translations[index];*/}
          {/*    const isLast = index === translations.length - 1;*/}
          {/*    if (isLast && !isFetchingMore && hasMoreToFetch) {*/}
          {/*      handleFetchMore();*/}
          {/*    }*/}

          {/*    const nsBannerAfter = nsBanners.find((b) => b.row === index + 1);*/}
          {/*    const nsBanner = nsBanners.find((b) => b.row === index);*/}
          {/*    return (*/}
          {/*      <div key={`${row.keyNamespace}.${row.keyId}`}>*/}
          {/*        {nsBanner && (*/}
          {/*          <NamespaceBanner*/}
          {/*            namespace={nsBanner}*/}
          {/*            maxWidth={columnSizes[0]}*/}
          {/*          />*/}
          {/*        )}*/}
          {/*        <RowTable*/}
          {/*          bannerBefore={Boolean(nsBanner)}*/}
          {/*          bannerAfter={Boolean(nsBannerAfter)}*/}
          {/*          data={row}*/}
          {/*          languages={languageCols}*/}
          {/*          columnSizes={columnSizesPercent}*/}
          {/*          onResize={startResize}*/}
          {/*        />*/}
          {/*      </div>*/}
          {/*    );*/}
          {/*  }}*/}
          {/*/>*/}
        </StyledContent>
      </StyledVerticalScroll>
    </BaseOrganizationSettingsView>
  );
};
