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
import { TYPE } from '@formatjs/icu-messageformat-parser';
import { IntlMessageFormat } from 'intl-messageformat';

const GLOBALS = {
  isCloud: true,
  instanceQualifier: 'Tolgee',
  instanceUrl: 'https://app.tolgee.io',
};

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

export default function t(
  key: string,
  defaultString?: string,
  demoParams?: Record<string, any>
) {
  const intl = new IntlMessageFormat(defaultString, 'en-US');
  const ast = intl.getAst();

  const stringArguments: string[] = [];

  for (const node of ast) {
    if (node.type === TYPE.literal || node.type === TYPE.pound) {
      continue;
    }

    if (node.type === TYPE.tag) {
      // TODO: find a way to process the tag
      continue;
    }

    stringArguments.push(`${node.value}: ${node.value.replace(/__/g, '.')}`);
  }

  const text =
    process.env.NODE_ENV === 'production'
      ? defaultString
      : formatDev(defaultString, demoParams);

  const messageExpression = stringArguments.length
    ? `#{${key}(\${ { ${stringArguments.join(', ')} } })}`
    : `#{${key}}`;

  return React.createElement('span', { 'th:utext': messageExpression }, text);
}
