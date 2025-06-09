/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import { ReactElement } from 'react';
import { MessageFormatElement, TYPE } from '@formatjs/icu-messageformat-parser';
import { IntlMessageFormat } from 'intl-messageformat';

declare const TranslatedTextSymbol: unique symbol;

export type TranslatedText = {
  [TranslatedTextSymbol]?: true;
  expr: string;
  text: string;
};

const GLOBALS = {
  isCloud: true,
  instanceQualifier: 'Tolgee',
  instanceUrl: 'https://app.tolgee.io',
};

const SeenIcuXmlIds = new Set<string>();

function formatDev(string?: string, demoParams?: Record<string, any>) {
  const formatted = new IntlMessageFormat(string, 'en-US').format({
    ...GLOBALS,
    ...demoParams,
  });

  if (Array.isArray(formatted)) {
    return formatted.map((e: string | boolean | object, i) =>
      typeof e === 'object' ? { ...e, key: i } : e
    ) as any;
  }

  return formatted;
}

function processMessageElements(
  id: string,
  elements: MessageFormatElement[],
  demoParams?: Record<string, any>
): [ReactElement[], Set<string>] {
  const fragments: ReactElement[] = [];
  const stringArguments = new Set<string>();

  for (const node of elements) {
    if (node.type === TYPE.literal || node.type === TYPE.pound) {
      // Text and what misc ICU syntax; not interesting
      continue;
    }

    if (node.type === TYPE.tag) {
      // Tag: needs to be converted to a Thymeleaf fragment
      const templateId = `${id}-${node.value}`;
      const [tagFrags, tagArgs] = processMessageElements(
        templateId,
        node.children
      );

      tagArgs.forEach((a) => stringArguments.add(a));
      if (!SeenIcuXmlIds.has(templateId)) {
        SeenIcuXmlIds.add(templateId);
        fragments.push(
          ...tagFrags,
          React.createElement(
            'th:block',
            {
              key: templateId,
              'th:fragment': `${templateId} (_children)`,
            },
            demoParams[node.value](
              React.createElement('div', { 'th:replace': '${_children}' })
            )
          )
        );
      }

      continue;
    }

    // Everything else is some form of variable: keep track of them
    stringArguments.add(node.value);
  }

  return [fragments, stringArguments];
}

function renderTranslatedText(
  key: string,
  defaultString: string,
  demoParams?: Record<string, any>
) {
  const id = `intl-${key.replace(/\./g, '__')}`;
  const intl = new IntlMessageFormat(defaultString, 'en-US');
  const ast = intl.getAst();

  const [fragments, stringArguments] = processMessageElements(
    id,
    ast,
    demoParams
  );

  const text =
    process.env.NODE_ENV === 'production'
      ? defaultString
      : formatDev(defaultString, demoParams);

  const stringArgs = Array.from(stringArguments);
  const stringArgsMap = stringArgs.map((a) => `${a}: ${a.replace(/__/g, '.')}`);

  const messageExpression = stringArgsMap.length
    ? `#{${key}(\${ { ${stringArgsMap.join(', ')} } })}`
    : `#{${key}}`;

  return { fragments, text, messageExpression };
}

export default function t(
  key: string,
  defaultString: string,
  demoParams?: Record<string, unknown>
) {
  const { fragments, text, messageExpression } = renderTranslatedText(
    key,
    defaultString,
    demoParams
  );

  return [
    ...fragments,
    React.createElement(
      'span',
      { key: 'render-el', 'th:utext': messageExpression },
      text
    ),
  ];
}

t.raw = function (
  key: string,
  defaultString: string,
  demoParams?: Record<string, any>
): TranslatedText {
  const { fragments, text, messageExpression } = renderTranslatedText(
    key,
    defaultString,
    demoParams
  );

  if (fragments.length)
    throw new Error('Invalid raw translation: cannot contain components.');

  return {
    expr: messageExpression,
    text,
  };
};

t.render = function (text: TranslatedText | string) {
  if (typeof text === 'string') return text;
  return React.createElement('span', { 'th:utext': text.expr }, text.text);
};
