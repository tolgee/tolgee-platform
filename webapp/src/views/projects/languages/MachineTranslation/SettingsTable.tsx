import { styled, Tooltip } from '@mui/material';
import { Help } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';

import { LanguageRow } from './LanguageRow';
import { TABLE_CENTERED, TABLE_DIVIDER, TABLE_TOP_ROW } from '../tableStyles';
import { LanguageCombinedSetting, OnMtChange } from './types';
import { PrimaryServiceLabel } from './PrimaryServiceLabel';

type Props = {
  settings: LanguageCombinedSetting[];
  expanded: boolean;
  onUpdate: OnMtChange;
};

export const SettingsTable = ({ settings, expanded, onUpdate }: Props) => {
  const { t } = useTranslate();

  const defaultSetting = settings.find((l) => l.id === null)!;

  return (
    <>
      <div className={TABLE_TOP_ROW} />
      <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
        <PrimaryServiceLabel />
      </div>
      <div className={clsx(TABLE_TOP_ROW)}>
        {t('project_languages_other_providers')}
      </div>
      <div className={clsx(TABLE_TOP_ROW)} />

      <LanguageRow
        rowData={{
          settings: defaultSetting,
          inheritedFromDefault: false,
          onChange: onUpdate,
        }}
      />

      {expanded && (
        <>
          <div className={TABLE_DIVIDER} />
          {settings
            .filter((l) => l.language)
            .map((langSettings) => {
              const inherited =
                !langSettings?.mtSettings && !langSettings?.autoSettings;

              return (
                <LanguageRow
                  key={langSettings.id}
                  rowData={{
                    settings: inherited
                      ? {
                          ...langSettings,
                          mtSettings: defaultSetting.mtSettings,
                          autoSettings: defaultSetting.autoSettings,
                        }
                      : langSettings,
                    inheritedFromDefault: inherited,
                    onChange: onUpdate,
                  }}
                />
              );
            })}
        </>
      )}
    </>
  );
};
