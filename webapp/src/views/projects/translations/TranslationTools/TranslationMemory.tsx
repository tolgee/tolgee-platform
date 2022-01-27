import { makeStyles } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { green, grey, orange } from '@material-ui/core/colors';
import { TabMessage } from './TabMessage';
import { useTranslationTools } from './useTranslationTools';

type PagedModelTranslationMemoryItemModel =
  components['schemas']['PagedModelTranslationMemoryItemModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
  },
  item: {
    display: 'grid',
    padding: theme.spacing(1, 1.25),
    gap: '0px 10px',
    gridTemplateColumns: 'auto 1fr',
    gridTemplateRows: 'auto auto 3px auto',
    gridTemplateAreas: `
      "target target"
      "base base"
      "space space"
      "similarity source"
    `,
    fontSize: 14,
    cursor: 'pointer',
    color: theme.palette.text.primary,
    transition: 'all 0.1s ease-in-out',
    transitionProperty: 'background color',
    '&:hover': {
      background: theme.palette.extraLightBackground.main,
      color: theme.palette.primary.main,
    },
  },
  target: {
    gridArea: 'target',
    fontSize: 15,
  },
  base: {
    gridArea: 'base',
    fontStyle: 'italic',
    color: theme.palette.text.secondary,
    fontSize: 13,
  },
  similarity: {
    gridArea: 'similarity',
    fontSize: 13,
    color: 'white',
    padding: '1px 9px',
    borderRadius: 10,
  },
  source: {
    gridArea: 'source',
    fontSize: 13,
    alignSelf: 'center',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    color: theme.palette.text.secondary,
  },
}));

type Props = {
  data: PagedModelTranslationMemoryItemModel | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
};

export const TranslationMemory: React.FC<Props> = ({ data, operationsRef }) => {
  const classes = useStyles();
  const t = useTranslate();
  const items = data?._embedded?.translationMemoryItems;

  if (!data) {
    return null;
  }

  return (
    <div className={classes.container}>
      {items?.length ? (
        items.map((item) => {
          const similarityColor =
            item.similarity === 1
              ? green[600]
              : item.similarity > 0.7
              ? orange[800]
              : grey[600];
          return (
            <div
              className={classes.item}
              key={item.keyName}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={() => {
                operationsRef.current.updateTranslation(item.targetText);
              }}
              role="button"
              data-cy="translation-tools-translation-memory-item"
            >
              <div className={classes.target}>{item.targetText}</div>
              <div className={classes.base}>{item.baseText}</div>
              <div
                className={classes.similarity}
                style={{ background: similarityColor }}
              >
                {Math.round(100 * item.similarity)}%
              </div>
              <div className={classes.source}>{item.keyName}</div>
            </div>
          );
        })
      ) : (
        <TabMessage
          type="placeholder"
          message={t({
            key: 'translation_tools_nothing_found',
            defaultValue: 'Nothing found',
          })}
        />
      )}
    </div>
  );
};
