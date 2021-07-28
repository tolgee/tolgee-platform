import { useMemo, useRef, useCallback, useEffect, useState } from 'react';
import ReactList from 'react-list';
import { makeStyles } from '@material-ui/core';
import { useContextSelector } from 'use-context-selector';
import { T } from '@tolgee/react';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { CellData } from './CellData';
import { resizeColumn, useResize } from '../useResize';
import { ColumnResizer } from '../ColumnResizer';
import { CellContent, CellPlain } from '../CellBase';
import { CellLanguage } from './CellLanguage';
import { SortableHeading } from './SortableHeading';
import { CellKey } from '../CellKey';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

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
    headerRow: {
      position: 'sticky',
      background: 'white',
      zIndex: 1,
      top: 0,
      borderWidth: '1px 0px 1px 0px',
      marginBottom: -1,
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
      overflow: 'hidden',
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
    },
  };
});

export const TranslationsTable = () => {
  const tableRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const resizersCallbacksRef = useRef<(() => void)[]>([]);

  const classes = useStyles();

  const projectPermissions = useProjectPermissions();

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

  const columns = useMemo(
    () => [
      {
        id: 'key',
        label: <T>translation_grid_key_text</T>,
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
      className={classes.table}
      ref={tableRef}
      data-cy="translations-view-table"
    >
      <div className={`${classes.rowWrapper} ${classes.headerRow}`}>
        <div className={classes.row}>
          <SortableHeading
            onSwap={handleColmnsSwap}
            columns={columns.map((col, i) => ({
              id: String(col.language?.tag || 'key'),
              width: columnSizes[i],
              draggable: Boolean(col.language),
              item: col.language ? (
                <div className={classes.headerCell}>
                  <CellLanguage
                    colIndex={i - 1}
                    onResize={handleResize}
                    language={col.language}
                  />
                </div>
              ) : (
                <div className={classes.keyCell}>
                  <CellPlain>
                    <CellContent>{col.label}</CellContent>
                  </CellPlain>
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
            passResizeCallback={(callback) =>
              (resizersCallbacksRef.current[i] = callback)
            }
          />
        );
      })}

      <ReactList
        ref={reactListRef}
        threshold={300}
        type="variable"
        itemSizeEstimator={(index, cache) => {
          return cache[index] || 62;
        }}
        length={translations.length}
        useTranslate3d
        itemRenderer={(index) => {
          const row = translations[index];
          const isLast = index === translations.length - 1;
          if (isLast && !isFetchingMore && hasMoreToFetch) {
            handleFetchMore();
          }
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
                          locale={col.language.tag}
                          keyName={row.keyName}
                          language={col.language.tag}
                          translation={row.translations[col.language.tag]}
                          width={columnSizes[i]}
                          editEnabled={projectPermissions.satisfiesPermission(
                            ProjectPermissionType.TRANSLATE
                          )}
                          colIndex={i - 1}
                          onResize={handleResize}
                        />
                      ) : (
                        <CellKey
                          keyId={row.keyId}
                          keyName={row.keyName}
                          text={col.accessor(row)}
                          screenshotCount={row.screenshotCount}
                          editEnabled={projectPermissions.satisfiesPermission(
                            ProjectPermissionType.EDIT
                          )}
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        }}
      />
    </div>
  );
};
