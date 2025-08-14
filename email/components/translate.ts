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

type ValidNode = Exclude<React.ReactNode, React.ReactPortal | Promise<unknown>>;

type Renderers = Record<string, (c: ValidNode) => ValidNode>;

export type MessageProps = Record<
  string,
  string | number | bigint | boolean | object | ((c: ValidNode) => ValidNode)
>;

const GLOBALS = {
  isCloud: true,
  instanceQualifier: 'Tolgee',
  backendUrl: 'https://app.tolgee.io',
};

let Counter = 0;
const SeenIcuXmlIds = new Set<string>();

const DEFAULT_RENDERERS: Renderers = {
  b: (c) => React.createElement('b', {}, c),
  i: (c) => React.createElement('i', {}, c),
  u: (c) => React.createElement('u', {}, c),
  em: (c) => React.createElement('em', {}, c),
  strong: (c) => React.createElement('strong', {}, c),
};

// https://stackoverflow.com/a/55387306
const interleave = (arr: any[], thing: any): any[] =>
  ([] as any[]).concat(...arr.map((n) => [n, thing])).slice(0, -1);

function addBrToTranslations(arg: ValidNode): ValidNode {
  // noinspection SuspiciousTypeOfGuard -- React.Fragment gate
  if (
    arg == null ||
    ['string', 'number', 'boolean', 'bigint'].includes(typeof arg)
  )
    return arg;

  if (typeof arg === 'string') {
    return interleave(
      arg.split('\n'),
      React.createElement('br', { key: `br-${++Counter}` })
    );
  }

  if (Array.isArray(arg)) {
    return arg.map((a) => addBrToTranslations(a));
  }

  if (
    typeof arg === 'object' &&
    'props' in arg &&
    arg.props &&
    typeof arg.props === 'object' &&
    'children' in arg.props
  ) {
    return React.cloneElement(arg as any, {
      children: addBrToTranslations(arg.props.children as any),
    });
  }

  return arg;
}

function formatDev(string: string, demoParams?: Record<string, any>) {
  const formatted = new IntlMessageFormat(string, 'en-US').format({
    ...GLOBALS,
    ...DEFAULT_RENDERERS,
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
      const renderer =
        demoParams?.[node.value] || DEFAULT_RENDERERS[node.value];

      if (!renderer) {
        throw new Error(
          `It is required to provide a render logic for XML tags! (in ${id} for tag ${node.value})`
        );
      }

      const fragmentId = `${id}--${node.value}`;
      const [tagFrags, tagArgs] = processMessageElements(
        fragmentId,
        node.children
      );

      tagArgs.forEach((a) => stringArguments.add(a));
      if (!SeenIcuXmlIds.has(fragmentId)) {
        SeenIcuXmlIds.add(fragmentId);

        fragments.push(
          ...tagFrags,
          React.createElement(
            'th:block',
            { key: fragmentId, 'th:fragment': `${fragmentId}(_children)` },
            renderer(
              React.createElement('th:block', {
                'th:replace': '${_children} ?: ~{}',
              })
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
  const id = `intl-${key.replace(/\./g, '--')}`;
  const intl = new IntlMessageFormat(defaultString, 'en-US');
  const ast = intl.getAst();

  if (process.env.NODE_ENV === 'production') {
    const [fragments, stringArguments] = processMessageElements(
      id,
      ast,
      demoParams
    );

    const stringArgs = Array.from(stringArguments);
    const stringArgsMap = stringArgs.map(
      (a) => `${a}: #strings.escapeXml(${a.replace(/__/g, '.')})`
    );

    const messageExpression = stringArgsMap.length
      ? `#{${key}(\${ { ${stringArgsMap.join(', ')} } })}`
      : `#{${key}}`;

    return { fragments, text: defaultString, messageExpression };
  }

  return {
    fragments: [],
    text: formatDev(defaultString, demoParams),
    messageExpression: '',
  };
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
      addBrToTranslations(text)
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
