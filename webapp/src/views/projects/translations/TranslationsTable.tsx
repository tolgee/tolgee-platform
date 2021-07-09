/* eslint-disable react/jsx-key */
import { useMemo, useRef, useCallback, useEffect } from 'react';
import { useContextSelector } from 'use-context-selector';
import { CircularProgress, Box, makeStyles } from '@material-ui/core';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { Cell } from './Cell';

const useStyles = makeStyles({
  table: {
    marginTop: 10,
    width: '100%',
    flexGrow: 0,
  },
  headerRow: {
    display: 'flex',
  },
  headerCell: {
    display: 'flex',
    flexGrow: 1,
    flexBasis: 0,
    alignItems: 'stretch',
    minWidth: '200px',
  },
  resizer: {
    width: 3,
    background: 'black',
  },
  dataRow: {
    display: 'flex',
  },
  cell: {
    boxSizing: 'border-box',
    display: 'flex',
    flexGrow: 1,
    flexBasis: 0,
    alignItems: 'stretch',
    minWidth: '200px',
    paddingRight: 5,
  },
});

export const TranslationsTable = () => {
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
      ...(languages?._embedded?.languages
        ?.filter((l) => data?.[0]?.translations[l.tag])
        .map((l) => ({
          Header: l.name,
          lang: l.tag,
          accessor: (item) => item.translations[l.tag]?.text,
        })) || []),
    ],
    [translations, languages]
  );

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
    <div className={classes.table}>
      <div className={classes.headerRow}>
        {columns.map((column, i) => (
          <div key={i} className={classes.headerCell}>
            {column.Header}
          </div>
        ))}
      </div>

      <div>
        {data.map((row) => {
          return (
            <div key={row.keyId} className={classes.dataRow}>
              {columns.map((col) => {
                return (
                  <div key={col.lang || 'key'} className={classes.cell}>
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
