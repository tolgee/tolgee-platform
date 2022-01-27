import { useCallback, useMemo, useState, useEffect, useRef } from 'react';
import { makeStyles } from '@material-ui/core';
import ReactList from 'react-list';

import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { useResize, resizeColumn } from '../useResize';
import { ColumnResizer } from '../ColumnResizer';
import { RowList } from './RowList';
import { TranslationsToolbar } from '../TranslationsToolbar';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'flex',
      position: 'relative',
      margin: '10px 0px 100px 0px',
      borderLeft: 0,
      borderRight: 0,
      background: 'white',
      flexGrow: 1,
      flexDirection: 'column',
      alignItems: 'stretch',
    },
  };
});

export const TranslationsList = () => {
  const classes = useStyles();
  const tableRef = useRef<HTMLDivElement>(null);
  const resizersCallbacksRef = useRef<(() => void)[]>([]);
  const reactListRef = useRef<ReactList>(null);
  const dispatch = useTranslationsDispatch();
  const translations = useTranslationsSelector((v) => v.translations);
  const languages = useTranslationsSelector((v) => v.languages);
  const translationsLanguages = useTranslationsSelector(
    (v) => v.translationsLanguages
  );
  const isFetchingMore = useTranslationsSelector((v) => v.isFetchingMore);
  const hasMoreToFetch = useTranslationsSelector((v) => v.hasMoreToFetch);
  const cursorKeyId = useTranslationsSelector((c) => c.cursor?.keyId);

  const [columnSizes, setColumnSizes] = useState([1, 3]);

  const { width } = useResize(tableRef, translations);

  const handleColumnResize = (i: number) => (size: number) => {
    setColumnSizes(resizeColumn(columnSizes, i, size, 0.25));
  };

  const columnSizesPercent = useMemo(() => {
    const columnsSum = columnSizes.reduce((a, b) => a + b, 0);
    return columnSizes.map((size) => (size / columnsSum) * 100 + '%');
  }, [columnSizes]);

  const handleResize = useCallback(
    (colIndex: number) => {
      resizersCallbacksRef.current[colIndex]?.();
    },
    [resizersCallbacksRef]
  );

  useEffect(() => {
    const previousWidth = columnSizes.reduce((a, b) => a + b, 0) || 1;
    const newSizes = columnSizes.map((w) => (w / previousWidth) * (width || 1));
    setColumnSizes(newSizes);
  }, [width]);

  const handleFetchMore = useCallback(() => {
    dispatch({
      type: 'FETCH_MORE',
    });
  }, [translations]);

  const languagesRow = useMemo(
    () =>
      (translationsLanguages
        ?.map((tag) => {
          return languages?.find((l) => l.tag === tag);
        })
        .filter(Boolean) as LanguageModel[]) || [],
    [languages, translationsLanguages]
  );

  useEffect(() => {
    if (reactListRef.current) {
      dispatch({
        type: 'REGISTER_LIST',
        payload: reactListRef.current,
      });
      return () => {
        dispatch({
          type: 'UNREGISTER_LIST',
          payload: reactListRef.current!,
        });
      };
    }
  }, [reactListRef.current]);

  if (!translations) {
    return null;
  }

  return (
    <div
      className={classes.container}
      style={{ marginBottom: cursorKeyId ? 500 : undefined }}
      ref={tableRef}
      data-cy="translations-view-list"
    >
      {columnSizes.slice(0, -1).map((w, i) => {
        const left = columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0);
        return (
          <ColumnResizer
            passResizeCallback={(callback) =>
              (resizersCallbacksRef.current[i] = callback)
            }
            key={i}
            size={w}
            left={left}
            onResize={handleColumnResize(i)}
          />
        );
      })}
      <ReactList
        ref={reactListRef}
        useTranslate3d
        threshold={800}
        type="variable"
        itemSizeEstimator={(index, cache) => {
          return (
            cache[index] ||
            Math.max((translationsLanguages?.length || 0) * 68, 83) + 1
          );
        }}
        // @ts-ignore
        scrollParentGetter={() => window}
        length={translations.length}
        itemRenderer={(index) => {
          const row = translations[index];
          const isLast = index === translations.length - 1;
          if (isLast && !isFetchingMore && hasMoreToFetch) {
            handleFetchMore();
          }
          return (
            <RowList
              key={row.keyId}
              data={row}
              languages={languagesRow}
              columnSizes={columnSizesPercent}
              onResize={handleResize}
            />
          );
        }}
      />
      <TranslationsToolbar width={width} />
    </div>
  );
};
