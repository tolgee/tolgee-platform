import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ReactList from 'react-list';
import { T } from '@tolgee/react';
import { makeStyles } from '@material-ui/core';
import { useContextSelector } from 'use-context-selector';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { resizeColumn, useResize } from '../useResize';
import { ColumnResizer } from '../ColumnResizer';
import { CellLanguage } from './CellLanguage';
import { SortableHeading } from './SortableHeading';
import { RowTable } from './RowTable';
import clsx from 'clsx';
import { TranslationsToolbar } from '../TranslationsToolbar';

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];
  return {
    container: {
      position: 'relative',
      margin: '10px 0px 100px 0px',
      borderLeft: 0,
      borderRight: 0,
      background: 'white',
      flexGrow: 1,
    },
    headerRow: {
      border: `1px solid ${borderColor}`,
      borderWidth: '1px 0px 1px 0px',
      position: 'sticky',
      background: 'white',
      zIndex: 1,
      top: 0,
      marginBottom: -1,
      display: 'flex',
    },
    resizer: {
      width: 3,
      background: 'black',
    },
    headerCell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexGrow: 0,
      alignItems: 'center',
      overflow: 'hidden',
    },
    keyCell: {
      paddingLeft: 13,
    },
  };
});

export const TranslationsTable = () => {
  const tableRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const resizersCallbacksRef = useRef<(() => void)[]>([]);

  const classes = useStyles();

  const dispatch = useTranslationsDispatch();
  const translations = useContextSelector(
    TranslationsContext,
    (v) => v.translations
  );
  const selectedLanguages =
    useContextSelector(TranslationsContext, (v) => v.selectedLanguages) || [];

  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const isFetchingMore = useContextSelector(
    TranslationsContext,
    (v) => v.isFetchingMore
  );
  const hasMoreToFetch = useContextSelector(
    TranslationsContext,
    (v) => v.hasMoreToFetch
  );
  const editKeyId = useContextSelector(
    TranslationsContext,
    (v) => v.edit?.keyId
  );

  useEffect(() => {
    // scroll to currently edited item
    if (editKeyId) {
      reactListRef.current?.scrollAround(
        translations!.findIndex((t) => t.keyId === editKeyId)
      );
    }
  }, [editKeyId]);

  const [columnsOrder, setColumnsOrder] = useState<string[]>([]);

  useEffect(() => {
    const newOrder = [
      ...columnsOrder?.filter((tag) => selectedLanguages.includes(tag)),
      ...selectedLanguages.filter((tag) => !columnsOrder.includes(tag)),
    ];
    setColumnsOrder(newOrder);
  }, [translations, languages]);

  const handleColmnsSwap = (a, b) => {
    const arr = [...columnsOrder];
    [arr[a], arr[b]] = [arr[b], arr[a]];
    setColumnsOrder(arr);
  };

  const languageCols = useMemo(() => {
    if (languages && columnsOrder) {
      return (
        columnsOrder?.map((lang) => {
          return languages.find((l) => l.tag === lang)!;
        }, [] as any[]) || []
      );
    } else {
      return [];
    }
  }, [columnsOrder, languages]);

  const columns = useMemo(
    () => [null, ...columnsOrder.map((tag) => tag)],
    [columnsOrder]
  );

  const [columnSizes, setColumnSizes] = useState(columns.map(() => 1));

  const { width } = useResize(tableRef, translations);

  const handleColumnResize = (i: number) => (size: number) => {
    setColumnSizes(resizeColumn(columnSizes, i, size));
  };

  const handleResize = useCallback(
    (colIndex: number) => {
      resizersCallbacksRef.current[colIndex]?.();
    },
    [resizersCallbacksRef]
  );

  useEffect(() => {
    const prevSizes =
      columnSizes.length === columns.length
        ? columnSizes
        : columns.map(() => 1);
    const previousWidth = prevSizes.reduce((a, b) => a + b, 0) || 1;
    const newSizes = prevSizes.map((w) => (w / previousWidth) * (width || 1));
    setColumnSizes(newSizes);
  }, [width, columns]);

  const handleFetchMore = useCallback(() => {
    dispatch({
      type: 'FETCH_MORE',
    });
  }, [translations]);

  if (!translations) {
    return null;
  }

  if (translations.length === 0) {
    return <EmptyListMessage />;
  }

  return (
    <div
      className={classes.container}
      ref={tableRef}
      data-cy="translations-view-table"
    >
      <div className={classes.headerRow}>
        <SortableHeading
          onSwap={handleColmnsSwap}
          columns={columns.map((tag, i) => {
            const language = languages!.find((lang) => lang.tag === tag)!;
            return {
              id: tag,
              width: columnSizes[i],
              draggable: Boolean(tag),
              item: tag ? (
                <div className={classes.headerCell}>
                  <CellLanguage
                    colIndex={i - 1}
                    onResize={handleResize}
                    language={language}
                  />
                </div>
              ) : (
                <div className={clsx(classes.headerCell, classes.keyCell)}>
                  <T>translation_grid_key_text</T>
                </div>
              ),
            };
          })}
        />
      </div>
      {columnSizes.slice(0, -1).map((w, i) => {
        const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
        return (
          <ColumnResizer
            key={i}
            size={w}
            left={left}
            onResize={handleColumnResize(i)}
            passResizeCallback={(callback) =>
              (resizersCallbacksRef.current[i] = callback)
            }
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
          return (
            <RowTable
              key={index}
              data={row}
              languages={languageCols}
              columnSizes={columnSizes}
              onResize={handleResize}
            />
          );
        }}
      />
      <TranslationsToolbar
        getVisibleRange={reactListRef.current?.getVisibleRange.bind(
          reactListRef.current
        )}
      />
    </div>
  );
};
