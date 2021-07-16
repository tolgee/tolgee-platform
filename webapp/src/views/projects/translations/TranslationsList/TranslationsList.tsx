import { useCallback, useMemo, useState, useEffect, useRef } from 'react';
import { makeStyles, Box, CircularProgress } from '@material-ui/core';
import ReactList from 'react-list';
import { useContextSelector } from 'use-context-selector';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../TranslationsContext';
import { CellKey } from '../CellKey';
import { LanguagesRow } from './LanguagesRow';
import { useResize, resizeColumn } from '../useResize';
import { ColumnResizer } from '../ColumnResizer';
import { ProjectPermissionType } from 'tg.service/response.types';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];
  return {
    table: {
      position: 'relative',
      margin: '10px -10px 0px -10px',
      borderLeft: 0,
      borderRight: 0,
      background: 'white',
      '& $rowWrapper:last-of-type': {
        borderWidth: '1px 0px 1px 0px',
      },
    },
    rowWrapper: {
      margin: `0px -${theme.spacing(2)}px 0px -${theme.spacing(2)}px`,
      padding: `0px ${theme.spacing(2)}px 0px ${theme.spacing(2)}px`,
      border: `1px solid ${borderColor}`,
      borderWidth: '1px 0px 0px 0px',
    },
    row: {
      display: 'flex',
    },
    headerCell: {
      boxSizing: 'border-box',
      display: 'flex',
      flexBasis: '30%',
      alignItems: 'stretch',
      flexGrow: 0,
      borderLeft: `1px solid ${borderColor}`,
    },
    keyCell: {
      display: 'flex',
      boxSizing: 'border-box',
      alignItems: 'stretch',
      flexGrow: 0,
      flexShrink: 0,
      overflow: 'hidden',
    },
    languages: {
      boxSizing: 'border-box',
      display: 'flex',
      alignItems: 'stretch',
      flexGrow: 0,
      flexShrink: 0,
      overflow: 'hidden',
      borderLeft: `1px solid ${borderColor}`,
    },
  };
});

export const TranslationsList = () => {
  const classes = useStyles();
  const tableRef = useRef<HTMLDivElement>(null);
  const projectPermissions = useProjectPermissions();
  const reactListRef = useRef<ReactList>(null);
  const dispatch = useTranslationsDispatch();
  const translations = useContextSelector(
    TranslationsContext,
    (v) => v.translations
  );
  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );
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

  const [columnSizes, setColumnSizes] = useState([1, 3]);

  const { width } = useResize(tableRef, translations);

  const handleColumnResize = (i: number) => (size: number) => {
    setColumnSizes(resizeColumn(columnSizes, i, size, 0.25));
  };

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
      (selectedLanguages
        ?.map((tag) => {
          return languages?.find((l) => l.tag === tag);
        })
        .filter(Boolean) as LanguageModel[]) || [],
    [languages, selectedLanguages]
  );

  if (!translations) {
    return null;
  }

  if (translations.length === 0) {
    return <EmptyListMessage />;
  }

  return (
    <div
      className={classes.table}
      ref={tableRef}
      data-cy="translations-view-list"
    >
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
      <ReactList
        ref={reactListRef}
        threshold={500}
        type="variable"
        itemSizeEstimator={(index, cache) => {
          const isLast = index === translations.length - 1;
          return (
            cache[index] ||
            // items count
            Math.max(selectedLanguages?.length || 0, 2) * 20 +
              // + padding
              20 +
              // + loading container if last
              (isLast ? 200 : 0)
          );
        }}
        length={translations.length}
        itemRenderer={(index) => {
          const row = translations[index];
          const isLast = index === translations.length - 1;
          if (isLast && !isFetchingMore && hasMoreToFetch) {
            handleFetchMore();
          }
          return (
            <div key={row.keyId} className={classes.rowWrapper}>
              <div className={classes.row}>
                <div
                  className={classes.keyCell}
                  style={{ flexBasis: columnSizes[0] }}
                >
                  <CellKey
                    keyId={row.keyId}
                    keyName={row.keyName}
                    text={row.keyName}
                    screenshotCount={row.screenshotCount}
                    editEnabled={projectPermissions.satisfiesPermission(
                      ProjectPermissionType.EDIT
                    )}
                  />
                </div>
                <div
                  className={classes.languages}
                  style={{ flexBasis: columnSizes[1] }}
                >
                  <LanguagesRow
                    width={columnSizes[1]}
                    languages={languagesRow}
                    data={row}
                    editEnabled={projectPermissions.satisfiesPermission(
                      ProjectPermissionType.TRANSLATE
                    )}
                  />
                </div>
              </div>
              {isLast && isFetchingMore && (
                <Box
                  display="flex"
                  justifyContent="center"
                  alignItems="center"
                  minHeight={200}
                >
                  <CircularProgress />
                </Box>
              )}
            </div>
          );
        }}
      />
    </div>
  );
};
