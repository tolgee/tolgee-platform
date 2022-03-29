import { makeStyles, Tooltip } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';
import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { getProviderImg } from '../TranslationTools/getProviderImg';

const useStyles = makeStyles((theme) => ({
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
}));

type Props = {
  provider?: string;
};

export const AutoTranslationIcon: React.FC<Props> = ({ provider }) => {
  const classes = useStyles();
  const providerImg = getProviderImg(provider);
  const t = useTranslate();

  return (
    <Tooltip
      title={
        provider
          ? t('translations_auto_translated_provider', {
              provider: provider,
            })
          : t('translations_auto_translated_tm')
      }
    >
      {provider && providerImg ? (
        <img src={providerImg} className={classes.providerImg} />
      ) : (
        <div className={classes.imgWrapper}>
          {provider ? (
            <MachineTranslationIcon className={classes.icon} />
          ) : (
            <TranslationMemoryIcon className={classes.icon} />
          )}
        </div>
      )}
    </Tooltip>
  );
};
