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
  each: string;
  demoIterations?: number;
  children: React.ReactElement;
};

export default function For({ each, demoIterations, children }: Props) {
  if (process.env.NODE_ENV === 'production') {
    return React.cloneElement(children, { 'data-th-each': each });
  }

  return React.createElement(
    React.Fragment,
    {},
    Array(demoIterations || 1)
      .fill(null)
      .map(() =>
        React.cloneElement(children, { key: Math.random().toString() })
      )
  );
}
