import { MessageFormat } from './formatGroups';
import React, { ReactNode } from 'react';
import { T } from '@tolgee/react';

export const messageFormatTranslation: Record<MessageFormat, ReactNode> = {
  C_SPRINTF: <T keyName="export_form_message_format_c-sprintf" />,
  PHP_SPRINTF: <T keyName="export_form_message_format_php-sprintf" />,
  JAVA_STRING_FORMAT: (
    <T keyName="export_form_message_format_java-string-format" />
  ),
  RUBY_SPRINTF: <T keyName="export_form_message_format_ruby-sprintf" />,
  I18NEXT: <T keyName="export_form_message_format_i18next" />,
  ICU: <T keyName="export_form_message_format_icu" />,
  APPLE_SPRINTF: <T keyName="export_form_message_format_apple-sprintf" />,
};
