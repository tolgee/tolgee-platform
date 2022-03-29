import { makeStyles } from '@material-ui/core';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { HistoryItem } from './HistoryItem';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'flex',
      flexDirection: 'column',
      flexGrow: 1,
      flexBasis: 100,
      overflow: 'hidden',
      position: 'relative',
    },
    scrollerWrapper: {
      flexGrow: 1,
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
    },
    reverseScroller: {
      display: 'flex',
      flexDirection: 'column-reverse',
      overflowY: 'auto',
      overflowX: 'hidden',
      overscrollBehavior: 'contain',
    },
    progressWrapper: {
      position: 'absolute',
      bottom: 0,
      left: 0,
      right: 0,
    },
  };
});

type Props = {
  keyId: number;
  language: LanguageModel;
  translation: TranslationViewModel | undefined;
  onCancel: () => void;
  editEnabled: boolean;
};

export const History: React.FC<Props> = ({ keyId, language, translation }) => {
  const classes = useStyles();
  const project = useProject();

  const history = useApiQuery({
    url: '/v2/projects/{projectId}/translations/{translationId}/history',
    method: 'get',
    path: { projectId: project.id, translationId: translation?.id as number },
    query: {},
    options: {
      enabled: Boolean(translation?.id),
    },
  });

  return (
    <div className={classes.container}>
      <div className={classes.scrollerWrapper}>
        <div className={classes.reverseScroller}>
          {history.data?._embedded?.revisions?.map((entry) => (
            <HistoryItem key={entry.timestamp} entry={entry} />
          ))}
        </div>
      </div>
      <div className={classes.progressWrapper}>
        <SmoothProgress loading={history.isFetching} />
      </div>
    </div>
  );
};
