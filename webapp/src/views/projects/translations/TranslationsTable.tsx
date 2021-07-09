import { useMemo, useRef, useCallback, useEffect, useState } from 'react';
import { useContextSelector } from 'use-context-selector';
import { CircularProgress, Box, makeStyles } from '@material-ui/core';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { CellData } from './CellData';
import { resizeColumn, useResize } from './tableTools';
import { ColumnResizer } from './ColumnResizer';
import { CellPlain } from './CellPlain';
import { CellLanguage } from './CellLanguage';
import { SortableHeading } from './SortableHeading';
import { CellKey } from './CellKey';

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.divider;
  return {
    table: {
      position: 'relative',
      margin: '10px -56px 0px -10px',
      borderLeft: 0,
      borderRight: 0,
      '& $rowWrapper:last-of-type': {
        borderWidth: '1px 0px 1px 0px',
      },
    },
    rowWrapper: {
      margin: '0px 0px 0px -46px',
      padding: '0px 0px 0px 46px',
      border: `1px solid ${borderColor}`,
      borderWidth: '1px 0px 0px 0px',
    },
    resizer: {
      width: 3,
      background: 'black',
    },
    row: {
      display: 'flex',
    },
    headerCell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexBasis: 1,
      alignItems: 'stretch',
      flexGrow: 0,
      borderLeft: `1px solid ${borderColor}`,
    },
    keyCell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexBasis: 1,
      alignItems: 'stretch',
      flexGrow: 0,
    },
    cell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexBasis: 1,
      alignItems: 'stretch',
      flexGrow: 0,
      overflow: 'hidden',
      '& + &': {
        borderLeft: `1px solid ${borderColor}`,
      },
    },
  };
});

export const TranslationsTable = () => {
  const tableRef = useRef<HTMLDivElement>(null);

  const classes = useStyles();
  const containerRef = useRef<HTMLDivElement>(null);

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

  const options = {
    root: null,
    rootMargin: '0px',
    treshold: 1.0,
  };

  const data = useMemo(() => translations || [], [translations]);

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

  const columns = useMemo(
    () => [
      {
        id: 'key',
        label: 'Klíč',
        language: undefined,
        accessor: (item) => item.keyName,
      },
      ...(columnsOrder?.map((tag) => {
        const lang = languages!.find((l) => l.tag === tag)!;
        return {
          id: String(lang.tag),
          label: lang.name,
          language: lang,
          accessor: (item) => item.translations[lang.tag]?.text,
        };
      }) || []),
    ],
    [columnsOrder]
  );

  const [columnSizes, setColumnSizes] = useState(columns.map(() => 1));

  const { width } = useResize(tableRef);

  const handleColumnResize = (i: number) => (size: number) => {
    setColumnSizes(resizeColumn(columnSizes, i, size));
  };

  useEffect(() => {
    const prevSizes =
      columnSizes.length === columns.length
        ? columnSizes
        : columns.map(() => 1);
    const previousWidth = prevSizes.reduce((a, b) => a + b, 0) || 1;
    const newSizes = prevSizes.map((w) => (w / previousWidth) * (width || 1));
    setColumnSizes(newSizes);
  }, [width, columns]);

  const handleFetchMore = useCallback(
    (e) => {
      if (e[0].isIntersecting && translations) {
        dispatch({
          type: 'FETCH_MORE',
        });
      }
    },
    [translations]
  );

  useEffect(() => {
    const observer = new IntersectionObserver(handleFetchMore, options);
    if (containerRef.current) {
      observer.observe(containerRef!.current);
    }
    return () => {
      if (containerRef.current) {
        observer.unobserve(containerRef.current);
      }
    };
  }, [handleFetchMore, containerRef]);

  if (!translations) {
    return null;
  }

  return (
    <div className={classes.table} ref={tableRef}>
      <div className={classes.rowWrapper}>
        <div className={classes.row}>
          <SortableHeading
            onSwap={handleColmnsSwap}
            columns={columns.map((col, i) => ({
              id: String(col.language?.tag || 'key'),
              width: columnSizes[i],
              draggable: Boolean(col.language),
              item: col.language ? (
                <div className={classes.headerCell}>
                  <CellLanguage language={col.language} />
                </div>
              ) : (
                <div className={classes.keyCell}>
                  <CellPlain>{col.label}</CellPlain>
                </div>
              ),
            }))}
          />
        </div>
      </div>
      {columnSizes.slice(0, -1).map((w, i) => {
        const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
        return (
          <ColumnResizer
            key={i}
            size={w}
            left={left}
            onResize={handleColumnResize(i)}
          />
        );
      })}

      <div>
        {data.map((row) => {
          return (
            <div key={row.keyId} className={classes.rowWrapper}>
              <div className={classes.row}>
                {columns.map((col, i) => {
                  return (
                    <div
                      key={col.language?.tag || 'key'}
                      className={classes.cell}
                      style={{ flexBasis: columnSizes[i] || 0 }}
                    >
                      {col.language ? (
                        <CellData
                          keyId={row.keyId}
                          keyName={row.keyName}
                          language={col.language?.tag}
                          text={col.accessor(row)}
                        />
                      ) : (
                        <CellKey
                          keyId={row.keyId}
                          keyName={row.keyName}
                          text={col.accessor(row)}
                          screenshotCount={row.screenshotCount}
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
      {hasMoreToFetch && (
        <>
          <div
            ref={containerRef}
            style={{
              position: 'relative',
              top: -200,
              height: 200,
              pointerEvents: 'none',
            }}
          />
          <Box display="flex" justifyContent="center" minHeight={200}>
            {isFetchingMore && <CircularProgress />}
          </Box>
        </>
      )}
    </div>
  );
};
