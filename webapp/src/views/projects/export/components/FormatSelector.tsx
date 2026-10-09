import { useField } from 'formik';
import { useTranslate } from '@tolgee/react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { Select } from 'tg.component/common/Select';

import React, { ReactNode } from 'react';
import { formatGroups, getFormatById } from './formatGroups';
import {
  CompactListSubheader,
  CompactMenuItem,
} from 'tg.component/ListComponents';

type Props = {
  className?: string;
};

export const FormatSelector: React.FC<React.PropsWithChildren<Props>> = ({
  className,
}) => {
  const { t } = useTranslate();
  const [field, _, fieldHelperProps] = useField('format');

  const options: ReactNode[] = [];

  formatGroups.forEach((group) => {
    options.push(
      <CompactListSubheader key={`g-${group.name}`} disableSticky>
        {group.name}
      </CompactListSubheader>
    );
    group.formats.forEach((option) =>
      options.push(
        <CompactMenuItem
          data-cy="export-format-selector-item"
          key={JSON.stringify(option)}
          value={option.id}
          onClick={stopAndPrevent(() => {
            fieldHelperProps.setValue(option.id);
          })}
        >
          {option.name}
        </CompactMenuItem>
      )
    );
  });

  return (
    <div className={className}>
      <Select
        label={t('export_translations_format_label')}
        minHeight={false}
        shrinkable
        renderValue={(value) => getFormatById(value as string).name}
        value={field.value}
        data-cy="export-format-selector"
        MenuProps={{
          variant: 'menu',
        }}
        displayEmpty
      >
        {options}
      </Select>
    </div>
  );
};
