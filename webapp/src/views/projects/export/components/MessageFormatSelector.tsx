import { useField, useFormikContext } from 'formik';
import { FormControl, InputLabel, Select } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';

import React, { useEffect } from 'react';
import { getFormatById, normalizeSelectedMessageFormat } from './formatGroups';
import { CompactMenuItem } from 'tg.component/ListComponents';
import { messageFormatTranslation } from './messageFormatTranslation';

type Props = {
  className: string;
};

export const MessageFormatSelector: React.FC<Props> = ({ className }) => {
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
    <FormControl className={className} variant="standard">
      <InputLabel>{t('export_translations_message_format_label')}</InputLabel>
      <Select
        renderValue={(value) => messageFormatTranslation[value]}
        value={field.value}
        data-cy="export-message-format-selector"
        MenuProps={{
          variant: 'menu',
        }}
        margin="dense"
        displayEmpty
      >
        {options}
      </Select>
    </FormControl>
  );
};
