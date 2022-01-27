import clsx from 'clsx';
import { makeStyles, Tooltip } from '@material-ui/core';
import { Clear } from '@material-ui/icons';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { getProviderImg } from '../TranslationTools/getProviderImg';

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
  icon: {
    fontSize: 16,
    color: '#249bad',
  },
  imgWrapper: {
    display: 'flex',
  },
  providerImg: {
    width: 14,
    height: 14,
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
    const providerImg = getProviderImg(translation.mtProvider);
    return (
      <div className={clsx(className, classes.wrapper)}>
        <div
          className={classes.container}
          data-cy="translations-auto-translated-indicator"
        >
          <Tooltip
            title={
              translation.mtProvider
                ? t('translations_auto_translated_provider', {
                    provider: translation.mtProvider,
                  })
                : t('translations_auto_translated_tm')
            }
          >
            {translation.mtProvider && providerImg ? (
              <img src={providerImg} className={classes.providerImg} />
            ) : (
              <div className={classes.imgWrapper}>
                {translation.mtProvider ? (
                  <MachineTranslationIcon className={classes.icon} />
                ) : (
                  <TranslationMemoryIcon className={classes.icon} />
                )}
              </div>
            )}
          </Tooltip>
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
