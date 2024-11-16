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
  condition: string;
  demoValue?: boolean;
  children: React.ReactElement | [React.ReactElement, React.ReactElement];
};

export default function If({
  condition,
  demoValue,
  children: _children,
}: Props) {
  const children = Array.isArray(_children) ? _children : [_children];

  if (process.env.NODE_ENV === 'production') {
    const trueCase = React.cloneElement(children[0], {
      'data-th-if': condition,
    });

    const falseCase =
      children.length === 2
        ? React.cloneElement(children[1], { 'data-th-unless': condition })
        : null;

    return React.createElement(React.Fragment, {}, trueCase, falseCase);
  }

  if (demoValue === false) return children[1];
  return children[0];
}
