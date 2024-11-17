/*
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

import type { ExtractedKey, ExtractionResult, Warning } from "@tolgee/cli/extractor";
import type { CallExpression, Expression, JSXAttribute, JSXAttrValue, JSXOpeningElement, Node, Span } from "@swc/types";
import { walk } from "estree-walker";
import { parse } from "@swc/core";

type AnyExpr = Expression | JSXAttrValue;

const isCallExpression = (node: Node): node is CallExpression =>
  node.type === 'CallExpression';
const isJsxOpeningElement = (node: Node): node is JSXOpeningElement =>
  node.type === 'JSXOpeningElement';

function spanToLine(code: string, span: Span) {
  return code.slice(0, span.start).split('\n').length;
}

function toString(node: Expression | JSXAttrValue): string | null {
  switch (node.type) {
    case 'StringLiteral':
      return node.value;
    case 'JSXText':
      return node.value.trim();
    case 'JSXExpressionContainer':
      return toString(node.expression);
  }

  return null;
}

function processTranslateCall(
  keyNameExpr: AnyExpr,
  defaultValueExpr: AnyExpr | null,
  line: number,
  code: string
): {
  key: ExtractedKey | null;
  warning: Warning | null;
} {
  const keyName = toString(keyNameExpr);
  const defaultValue = defaultValueExpr ? toString(defaultValueExpr) : null;

  if (!keyName) {
    return {
      key: null,
      warning: {
        warning: `Failed to extract the key. It may be dynamic or not yet recognized during parse. AST node was a \`${keyNameExpr.type}\``,
        line: 'span' in keyNameExpr ? spanToLine(code, keyNameExpr.span) : line,
      },
    };
  }

  return {
    key: {
      keyName,
      defaultValue,
      line,
    },
    warning:
      !defaultValue && defaultValueExpr
        ? {
            warning: `Failed to extract the default value. It may be dynamic or not yet recognized during parse. AST node was a \`${keyNameExpr.type}\``,
            line:
              'span' in defaultValueExpr
                ? spanToLine(code, defaultValueExpr.span)
                : line,
          }
        : null,
  };
}

export default async function extractor(
  code: string
): Promise<ExtractionResult> {
  const module = await parse(code, {
    syntax: 'typescript',
    tsx: true,
  });

  const keys: ExtractedKey[] = [];
  const warnings: Warning[] = [];
  walk(module as any, {
    enter(node: Node) {
      if (
        isCallExpression(node) &&
        node.callee.type === 'Identifier' &&
        node.callee.value === 't'
      ) {
        const line = spanToLine(code, node.span);
        const res = processTranslateCall(
          node.arguments[0].expression,
          node.arguments[1].expression,
          line,
          code
        );

        if (res.key) keys.push(res.key)
        if (res.warning) warnings.push(res.warning)
      }

      if (
        isJsxOpeningElement(node) &&
        node.name.type === 'Identifier' &&
        node.name.value === 'LocalizedText'
      ) {
        const line = spanToLine(code, node.span);
        const keyName = node.attributes.find(
          (e): e is JSXAttribute =>
            e.type === 'JSXAttribute' &&
            e.name.type === 'Identifier' &&
            e.name.value === 'keyName'
        );
        const defaultValue = node.attributes.find(
          (e): e is JSXAttribute =>
            e.type === 'JSXAttribute' &&
            e.name.type === 'Identifier' &&
            e.name.value === 'defaultValue'
        );

        if (!keyName) {
          warnings.push({
            warning:
              'Found <LocalizedText/> but could not find its `keyName` attribute. This likely means the extraction will be incomplete.',
            line: spanToLine(code, node.span),
          });

          // We can't proceed, abort here
          return;
        }

        if (!defaultValue) {
          warnings.push({
            warning:
              'Found <LocalizedText/> but could not find its `defaultValue` attribute.',
            line: spanToLine(code, node.span),
          });
        }

        const res = processTranslateCall(
          keyName.value,
          defaultValue.value,
          line,
          code
        );

        if (res.key) keys.push(res.key)
        if (res.warning) warnings.push(res.warning)
      }
    },
  });

  return { keys, warnings };
}
