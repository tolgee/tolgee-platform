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

type Props = {
  variable: string;
  demoValue: string;
  injectHtml?: boolean;
};

export default function Var({ variable, demoValue, injectHtml }: Props) {
  const attr = injectHtml ? 'th:utext' : 'th:text';

  if (process.env.NODE_ENV === 'production') {
    // This is rendered using the same two-pass trick used in translate.ts
    // It's done to prevent this from being a potential vector of injection
    return React.createElement('th:block', {
      'th:utext': `'<span ${attr}="\${${variable}}"></span>'`,
    });
  }

  return React.createElement(
    'span',
    { [injectHtml ? 'th:utext' : 'th:text']: `\${${variable}}` },
    demoValue
  );
}
