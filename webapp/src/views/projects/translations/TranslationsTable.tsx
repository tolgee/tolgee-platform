import { useMemo, useRef, useCallback, useEffect, useState } from 'react';
import { useContextSelector } from 'use-context-selector';
import { CircularProgress, Box, makeStyles } from '@material-ui/core';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { Cell } from './Cell';
import { resizeColumn, useResize } from './tableTools';
import { ColumnResizer } from './ColumnResizer';

const useStyles = makeStyles(() => {
  return {
    table: {
      marginTop: 10,
      width: '100%',
      position: 'relative',
    },
    headerRow: {
      display: 'flex',
      position: 'relative',
    },
    resizer: {
      width: 3,
      background: 'black',
    },
    dataRow: {
      display: 'flex',
    },
    headerCell: {
      display: 'flex',
      flexBasis: 1,
      alignItems: 'stretch',
      position: 'relative',
    },
    cell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexBasis: 1,
      alignItems: 'stretch',
      paddingRight: 5,
      flexGrow: 0,
      overflow: 'hidden',
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

  const columns = useMemo(
    () => [
      { Header: 'Klíč', lang: undefined, accessor: (item) => item.keyName },
      ...(languages
        ?.filter((l) => data?.[0]?.translations[l.tag])
        .map((l) => ({
          Header: l.name,
          lang: l.tag,
          accessor: (item) => item.translations[l.tag]?.text,
        })) || []),
    ],
    [translations, languages]
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

  return (
    <div className={classes.table} ref={tableRef}>
      <div className={classes.headerRow}>
        {columns.map((column, i) => (
          <div
            key={i}
            className={classes.headerCell}
            style={{ flexBasis: columnSizes[i] || 0 }}
          >
            {column.Header}
          </div>
        ))}
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
            <div key={row.keyId} className={classes.dataRow}>
              {columns.map((col, i) => {
                return (
                  <div
                    key={col.lang || 'key'}
                    className={classes.cell}
                    style={{ flexBasis: columnSizes[i] || 0 }}
                  >
                    <Cell
                      keyId={row.keyId}
                      keyName={row.keyName}
                      language={col.lang}
                      text={col.accessor(row)}
                    />
                  </div>
                );
              })}
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
