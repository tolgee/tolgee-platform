/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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
import IntlMessageFormat from 'intl-messageformat';

const GLOBALS = {
  isCloud: true,
  instanceQualifier: 'Tolgee',
  instanceUrl: 'https://app.tolgee.io',
};

export default function t(
  key: string,
  defaultString?: string,
  demoParams?: Record<string, any>
) {
  const text =
    process.env.NODE_ENV === 'production'
      ? defaultString
      : new IntlMessageFormat(defaultString, 'en-US').format({
          ...GLOBALS,
          ...demoParams,
        });

  return React.createElement('span', { 'th:utext': `#{${key}}` }, text);
}
