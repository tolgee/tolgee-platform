import { makeStyles } from '@material-ui/core';
import { components } from 'tg.service/apiSchema.generated';
import { getProviderImg } from './getProviderImg';
import { useTranslationTools } from './useTranslationTools';

type SuggestResultModel = components['schemas']['SuggestResultModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
  },
  item: {
    padding: theme.spacing(1, 1.25),
    display: 'grid',
    gap: theme.spacing(0, 1),
    gridTemplateColumns: '20px 1fr',
    cursor: 'pointer',
    transition: 'all 0.1s ease-in-out',
    transitionProperty: 'background color',
    '&:hover': {
      background: theme.palette.extraLightBackground.main,
      color: theme.palette.primary.main,
    },
  },
  source: {
    marginTop: 3,
  },
  value: {
    fontSize: 15,
    alignSelf: 'center',
  },
}));

type Props = {
  data: SuggestResultModel | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
};

export const MachineTranslation: React.FC<Props> = ({
  data,
  operationsRef,
}) => {
  const classes = useStyles();
  const items = data?.machineTranslations
    ? Object.entries(data?.machineTranslations)
    : [];

  return (
    <div className={classes.container}>
      {items?.map(([provider, translation]) => {
        const providerImg = getProviderImg(provider);

        return (
          <div
            className={classes.item}
            key={provider}
            onMouseDown={(e) => {
              e.preventDefault();
            }}
            onClick={() => {
              operationsRef.current.updateTranslation(translation);
            }}
            role="button"
            data-cy="translation-tools-machine-translation-item"
          >
            <div className={classes.source}>
              {providerImg && <img src={providerImg} width="16px" />}
            </div>
            <div className={classes.value}>{translation}</div>
          </div>
        );
      })}
    </div>
  );
};
