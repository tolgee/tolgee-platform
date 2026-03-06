import { useCallback, useEffect, useMemo, useRef } from 'react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { ColumnResizer } from '../ColumnResizer';
import { CellLanguage } from './CellLanguage';
import { RowTable } from './RowTable';
import { NamespaceBanner } from '../Namespace/NamespaceBanner';
import { useNsBanners } from '../context/useNsBanners';
import { NAMESPACE_BANNER_SPACING } from '../cell/styles';
import { ReactList } from 'tg.component/reactList/ReactList';
import { useColumns } from '../useColumns';
import { useHorizontalScroll } from '../useHorizontalScroll';
import { ScrollableTableContainer } from '../ScrollableTableContainer';

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
  &.keyCell {
    padding-left: 13px;
    padding-top: 8px;
  }
`;

type Props = {
  width: number;
};

export const TranslationsTable = ({ width }: Props) => {
  const mainContentWidth = useTranslationsSelector(
    (c) => c.layout.mainContentWidth
  );
  const reactListRef = useRef<ReactList>(null);

  const { fetchMore, registerList, unregisterList } = useTranslationsActions();
  const translations = useTranslationsSelector((v) => v.translations);
  const translationsLanguages =
    useTranslationsSelector((v) => v.translationsLanguages) || [];

  const languages = useTranslationsSelector((v) => v.languages);
  const isFetchingMore = useTranslationsSelector((v) => v.isFetchingMore);
  const hasMoreToFetch = useTranslationsSelector((v) => v.hasMoreToFetch);

  const languageCols = useMemo(() => {
    if (languages && translationsLanguages) {
      return (
        translationsLanguages?.map((lang) => {
          return languages.find((l) => l.tag === lang)!;
        }, [] as any[]) || []
      ).filter(Boolean);
    } else {
      return [];
    }
  }, [translationsLanguages, languages]);

  const columns = useMemo(
    () => [null, ...translationsLanguages.map((tag) => tag)],
    [translationsLanguages]
  );

  const {
    columnSizes,
    columnSizesPercent,
    startResize,
    resizeColumn,
    addResizer,
  } = useColumns({
    width,
    initialRatios: columns.map(() => 1),
    minSize: 350,
  });

  const handleFetchMore = useCallback(() => {
    fetchMore();
  }, [translations]);

  useEffect(() => {
    if (reactListRef.current) {
      registerList(reactListRef.current);
      return () => {
        unregisterList(reactListRef.current!);
      };
    }
  }, [reactListRef.current]);

  const nsBanners = useNsBanners();
  const isBannerOnFirstRow = nsBanners[0]?.row === 0;

  const horizontalScroll = useHorizontalScroll(columnSizes, mainContentWidth);

  if (!translations) {
    return null;
  }

  return (
    <ScrollableTableContainer
      data-cy="translations-view-table"
      {...horizontalScroll}
    >
      <StyledContent>
        <StyledHeaderRow
          data-cy="this-is-the-element"
          style={{
            gridTemplateColumns: columnSizesPercent.join(' '),
          }}
        >
          {columns.map((tag, i) => {
            const language = languages?.find((lang) => lang.tag === tag);
            return tag ? (
              language && (
                <StyledHeaderCell key={i}>
                  <CellLanguage
                    onResize={() => startResize(i - 1)}
                    language={language}
                  />
                </StyledHeaderCell>
              )
            ) : (
              <StyledHeaderCell
                key={i}
                style={{
                  width: columnSizesPercent[i],
                  height:
                    39 + (isBannerOnFirstRow ? NAMESPACE_BANNER_SPACING : 0),
                }}
                className="keyCell"
              >
                <T keyName="translation_grid_key_text" />
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

        <ReactList
          ref={reactListRef}
          threshold={800}
          type="variable"
          itemSizeEstimator={(index, cache) => {
            return cache[index] || 84;
          }}
          // @ts-ignore
          scrollParentGetter={() => window}
          length={translations.length}
          useTranslate3d
          itemRenderer={(index) => {
            const row = translations[index];
            const isLast = index === translations.length - 1;
            if (isLast && !isFetchingMore && hasMoreToFetch) {
              handleFetchMore();
            }

            const nsBannerAfter = nsBanners.find((b) => b.row === index + 1);
            const nsBanner = nsBanners.find((b) => b.row === index);
            return (
              <div key={`${row.keyNamespace}.${row.keyId}`}>
                {nsBanner && (
                  <NamespaceBanner
                    namespace={nsBanner}
                    maxWidth={columnSizes[0]}
                  />
                )}
                <RowTable
                  bannerBefore={Boolean(nsBanner)}
                  bannerAfter={Boolean(nsBannerAfter)}
                  data={row}
                  languages={languageCols}
                  columnSizes={columnSizesPercent}
                  onResize={startResize}
                />
              </div>
            );
          }}
        />
      </StyledContent>
    </ScrollableTableContainer>
  );
};
