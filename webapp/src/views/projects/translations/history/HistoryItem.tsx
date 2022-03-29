import { makeStyles, Tooltip } from '@material-ui/core';
import { useCurrentLanguage, useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { translationStates } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { AutoTranslationIcon } from '../cell/AutoTranslationIcon';
import { LimitedHeightText } from '../LimitedHeightText';

type TranslationHistoryModel = components['schemas']['TranslationHistoryModel'];
type revisionType =
  components['schemas']['TranslationHistoryModel']['revisionType'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'grid',
      padding: '7px 12px 7px 12px',
      gridTemplateAreas: `
        "avatar state text   time          "
        "avatar .     text   autoIndicator "
      `,
      gridTemplateColumns: 'auto auto 1fr auto',
      gridTemplateRows: 'auto 1fr',
      background: 'transparent',
      transition: 'background 0.1s ease-out',
      '&:hover': {
        background: theme.palette.extraLightBackground.main,
        transition: 'background 0.1s ease-in',
      },
      '&:hover $hoverVisible': {
        opacity: 1,
        transition: 'opacity 0.5s ease-in',
      },
    },
    avatar: {
      gridArea: 'avatar',
      margin: theme.spacing(0.5, 1, 0.5, 0),
      alignSelf: 'start',
    },
    dot: {
      marginTop: 12,
      marginRight: 4,
      gridArea: 'dot',
      width: 8,
      height: 8,
      borderRadius: '50%',
    },
    text: {
      gridArea: 'text',
      padding: 0,
      margin: 0,
      marginTop: 7,
      marginBottom: 4,
      lineHeight: 1.1,
      fontFamily: theme.typography.fontFamily,
      whiteSpace: 'pre-wrap',
      wordWrap: 'break-word',
    },
    placeholder: {
      color: theme.palette.text.secondary,
    },
    state: {
      gridArea: 'state',
    },
    autoIndicator: {
      gridArea: 'autoIndicator',
      justifySelf: 'end',
    },
    time: {
      gridArea: 'time',
      display: 'flex',
      fontSize: '11px',
      justifyContent: 'flex-end',
    },
  };
});

type Props = {
  entry: TranslationHistoryModel;
};

export const HistoryItem: React.FC<Props> = ({ entry }) => {
  const classes = useStyles();
  const lang = useCurrentLanguage();
  const t = useTranslate();

  const date = new Date(entry.timestamp);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();

  const translationState = translationStates[entry.state!];

  const getHistoryAction = (type: revisionType) => {
    switch (type) {
      case 'ADD':
        return t('history_revision_type_add');
      default:
        return t('history_revision_type_mod');
    }
  };

  const actionLabel = getHistoryAction(entry.revisionType);

  return (
    <div className={classes.container} data-cy="translation-history-item">
      <Tooltip title={entry.author?.name || entry.author?.username || ''}>
        <div className={classes.avatar}>
          <AvatarImg
            owner={{
              ...entry.author,
              type: 'USER',
              id: entry.author?.id as number,
            }}
            size={24}
            autoAvatarType="IDENTICON"
            circle
          />
        </div>
      </Tooltip>
      <div className={classes.text}>
        <LimitedHeightText maxLines={3}>
          <span>
            {entry.text}
            {entry.text ? ' ' : ''}
            <span className={classes.placeholder}>
              ({actionLabel.toLowerCase()})
            </span>
          </span>
        </LimitedHeightText>
      </div>
      <div className={classes.state}>
        <Tooltip title={t(translationState.translationKey)}>
          <div
            className={classes.dot}
            style={{ background: translationState.color }}
          />
        </Tooltip>
      </div>
      <div className={classes.time}>
        {!isToday && date.toLocaleDateString(lang()) + ' '}
        {date.toLocaleTimeString(lang(), {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </div>
      {entry.auto && (
        <div className={classes.autoIndicator}>
          <AutoTranslationIcon provider={entry.mtProvider} />
        </div>
      )}
    </div>
  );
};
