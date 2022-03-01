import { makeStyles, Tooltip } from '@material-ui/core';
import { Clear } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { MachineTranslationIcon } from 'tg.component/CustomIcons';
import { useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsDispatch } from '../context/TranslationsContext';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'inline-flex',
    gap: 2,
    alignItems: 'center',
    width: 70,
    height: 14,
    '&:hover $clearButton': {
      display: 'block',
    },
  },
  icon: {
    fontSize: 12,
    color: '#249bad',
  },
  clearButton: {
    fontSize: 16,
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
  const t = useTranslate();
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
    clear
      .mutateAsync({
        path: { projectId: project.id, translationId: translation!.id },
      })
      .then(() => {
        dispatch({
          type: 'UPDATE_TRANSLATION',
          payload: { keyId: keyData.keyId, lang, data: { auto: false } },
        });
      });
  };

  if (translation?.auto) {
    return (
      <div className={className}>
        <Tooltip
          title={
            translation.mtProvider
              ? t('translations_auto_translated_provider', {
                  provider: translation.mtProvider,
                })
              : t('translations_auto_translated_tm')
          }
        >
          <div
            className={classes.container}
            data-cy="translations-auto-translated-indicator"
          >
            <MachineTranslationIcon className={classes.icon} />
            <Clear
              role="button"
              className={classes.clearButton}
              onClick={handleClear}
              data-cy="translations-auto-translated-clear-button"
            />
          </div>
        </Tooltip>
      </div>
    );
  } else {
    return null;
  }
};
