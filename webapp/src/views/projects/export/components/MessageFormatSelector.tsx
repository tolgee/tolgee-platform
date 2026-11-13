import { useField, useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { Select } from 'tg.component/common/Select';

import React, { useEffect } from 'react';
import {
  getFormatById,
  MessageFormat,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';
import { CompactMenuItem } from 'tg.component/ListComponents';
import { messageFormatTranslation } from './messageFormatTranslation';

type Props = {
  className?: string;
};

export const MessageFormatSelector: React.FC<
  React.PropsWithChildren<Props>
> = ({ className }) => {
  const { t } = useTranslate();
  const [field, _, fieldHelperProps] = useField('messageFormat');

  const formikContext = useFormikContext<{ format: string }>();
  const formatId = formikContext.values['format'];

  const selectedFormat = getFormatById(formatId);
  const supportedMessageFormats = selectedFormat.supportedMessageFormats;

  useEffect(() => {
    const newValue = normalizeSelectedMessageFormat({
      format: formatId,
      messageFormat: field.value,
    });
    if (newValue !== field.value) {
      fieldHelperProps.setValue(newValue);
    }
  }, [formatId]);

  if (supportedMessageFormats == null) {
    return null;
  }

  const options = supportedMessageFormats.map((option) => (
    <CompactMenuItem
      data-cy="export-message-format-selector-item"
      key={option}
      value={option}
      onClick={stopAndPrevent(() => {
        fieldHelperProps.setValue(option);
      })}
    >
      {messageFormatTranslation[option]}
    </CompactMenuItem>
  ));

  return (
    <div className={className}>
      <Select
        label={t('export_translations_message_format_label')}
        minHeight={false}
        shrinkable
        renderValue={(value) =>
          messageFormatTranslation[value as MessageFormat]
        }
        value={field.value}
        data-cy="export-message-format-selector"
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
