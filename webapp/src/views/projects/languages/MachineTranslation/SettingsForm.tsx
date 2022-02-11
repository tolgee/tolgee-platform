import { makeStyles, Tooltip } from '@material-ui/core';
import { Help } from '@material-ui/icons';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import { components } from 'tg.service/apiSchema.generated';
import { useTableStyles } from '../tableStyles';
import { LanguageRow } from './LanguageRow';

const useStyles = makeStyles((theme) => ({
  primaryProvider: {
    display: 'flex',
    gap: 4,
    alignItems: 'center',
  },
  helpIcon: {
    fontSize: 15,
  },
}));

type PagedModelLanguageModel = components['schemas']['PagedModelLanguageModel'];

type Props = {
  providers: string[];
  expanded: boolean;
  languages: PagedModelLanguageModel | undefined;
};

export const SettingsForm = ({ providers, expanded, languages }: Props) => {
  const classes = useStyles();
  const tableClasses = useTableStyles();
  const t = useTranslate();

  return (
    <>
      <div className={tableClasses.topRow} />
      {providers.map((provider) => (
        <div
          key={provider}
          className={clsx(tableClasses.topRow, tableClasses.centered)}
        >
          {provider}
        </div>
      ))}
      <div className={clsx(tableClasses.topRow, tableClasses.centered)}>
        <Tooltip title={t('project_languages_primary_provider_hint')}>
          <div className={classes.primaryProvider}>
            <div>
              {t({
                key: 'project_languages_primary_provider',
                defaultValue: 'Primary',
              })}
            </div>
            <Help className={classes.helpIcon} />
          </div>
        </Tooltip>
      </div>
      <LanguageRow lang={null} providers={providers} />

      {expanded && (
        <>
          <div className={tableClasses.divider} />
          {languages?._embedded?.languages
            ?.filter(({ base }) => !base)
            .map((lang) => (
              <LanguageRow key={lang.id} lang={lang} providers={providers} />
            ))}
        </>
      )}
    </>
  );
};
