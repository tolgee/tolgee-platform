import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';
import { Clear } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { AutoTranslationIcon } from './AutoTranslationIcon';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => ({
  wrapper: {
    height: 0,
  },
  container: {
    display: 'inline-flex',
    flexGrow: 0,
    alignItems: 'center',
    height: 20,
    border: `1px solid transparent`,
    padding: '0px 4px',
    marginLeft: -4,
    borderRadius: 10,
    '&:hover $clearButton': {
      display: 'block',
    },
    '&:hover': {
      border: `1px solid ${theme.palette.lightDivider.main}`,
      transition: 'all 0.1s',
    },
  },
  clearButton: {
    paddingLeft: 2,
    fontSize: 18,
    display: 'none',
  },
}));

type Props = {
  keyData: KeyWithTranslationsModel;
  lang: string;
  className?: string;
};

export const AutoTranslationIndicator: React.FC<Props> = ({
  keyData,
  lang,
  className,
}) => {
  const classes = useStyles();
  const project = useProject();
  const translation = keyData.translations[lang];

  const dispatch = useTranslationsDispatch();

  const clear = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/dismiss-auto-translated-state',
    method: 'put',
  });

  const handleClear = (e: React.MouseEvent<SVGSVGElement, MouseEvent>) => {
    e.stopPropagation();
    clear.mutateAsync(
      {
        path: { projectId: project.id, translationId: translation!.id },
      },
      {
        onSuccess() {
          dispatch({
            type: 'UPDATE_TRANSLATION',
            payload: { keyId: keyData.keyId, lang, data: { auto: false } },
          });
        },
      }
    );
  };

  if (translation?.auto) {
    return (
      <div className={clsx(className, classes.wrapper)}>
        <div
          className={classes.container}
          data-cy="translations-auto-translated-indicator"
        >
          <AutoTranslationIcon provider={translation.mtProvider} />
          <Clear
            role="button"
            className={classes.clearButton}
            onClick={handleClear}
            data-cy="translations-auto-translated-clear-button"
          />
        </div>
      </div>
    );
  } else {
    return null;
  }
};
