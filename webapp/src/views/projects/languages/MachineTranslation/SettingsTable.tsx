import clsx from 'clsx';

import {
  TABLE_CENTERED,
  TABLE_DIVIDER,
  TABLE_TOP_ROW,
} from '../../../../component/languages/tableStyles';
import { LanguageRow } from './LanguageRow';
import { LanguageCombinedSetting, OnMtChange } from './types';
import { PrimaryServiceLabel } from './PrimaryServiceLabel';
import { SuggestionsLabel } from './SuggestionsLabel';

type Props = {
  settings: LanguageCombinedSetting[];
  onUpdate: OnMtChange;
};

export const SettingsTable = ({ settings, onUpdate }: Props) => {
  const defaultSetting = settings.find((l) => l.id === null)!;

  return (
    <>
      <div className={TABLE_TOP_ROW} />
      <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
        <PrimaryServiceLabel />
      </div>
      <div className={clsx(TABLE_TOP_ROW)}>
        <SuggestionsLabel />
      </div>
      <div className={clsx(TABLE_TOP_ROW)} />

      <LanguageRow
        rowData={{
          settings: defaultSetting,
          inheritedFromDefault: false,
          onChange: onUpdate,
        }}
      />

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
  );
};
