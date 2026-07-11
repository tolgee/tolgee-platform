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

  if (children[0].type !== If.Then) {
    // eslint-disable-next-line no-console
    console.warn(
      'Warning: not using <If.Then /> as first child of <If /> is discouraged.'
    );
  }

  if (children.length > 1 && children[1].type !== If.Else) {
    // eslint-disable-next-line no-console
    console.warn(
      'Warning: not using <If.Else /> as second child of <If /> is discouraged.'
    );
  }

  if (process.env.NODE_ENV === 'production') {
    const trueCase = React.createElement(
      'th:block',
      {
        key: 'true-case',
        'th:if': condition,
      },
      children[0]
    );

    const falseCase = React.createElement(
      'th:block',
      {
        key: 'false-case',
        'th:unless': condition,
      },
      children[1]
    );

    return [trueCase, children.length === 2 ? falseCase : null];
  }

  if (demoValue === false) return children[1];
  return children[0];
}

function IfThen(props: React.Attributes) {
  return React.createElement(React.Fragment, props);
}

function IfElse(props: React.Attributes) {
  return React.createElement(React.Fragment, props);
}

If.Then = IfThen;
If.Else = IfElse;
