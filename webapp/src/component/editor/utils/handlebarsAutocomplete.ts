import {
  Completion,
  CompletionContext,
  CompletionResult,
  CompletionSource,
} from '@codemirror/autocomplete';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';
import { components } from 'tg.service/apiSchema.generated';

type PromptVariableDto = components['schemas']['PromptVariableDto'];

export function trimDetail(text: string | undefined, maxLength = 20) {
  return text && text.length > maxLength
    ? `${text.slice(0, maxLength - 3)}...`
    : text;
}

function getValue(context: CompletionContext, from: number, to?: number) {
  return context.state.doc.toString().substring(from, to);
}

export const handlebarsAutocomplete =
  (
    variablesRef: RefObject<PromptVariableDto[] | undefined>,
    helpersRef?: RefObject<Completion[] | undefined>
  ): CompletionSource =>
  (context) => {
    const tree = syntaxTree(context.state);
    const nodeBefore = tree.resolveInner(context.pos, -1);
    const nodeAfter = tree.resolveInner(context.pos, 1);

    let from: CompletionResult['from'] | undefined = undefined;
    let to: CompletionResult['to'] = undefined;

    let path: string[] = [];
    let isNameSlot = false;

    if (nodeBefore.name === '{{') {
      from = context.pos;
      isNameSlot = true;
    }

    if (
      nodeBefore.name === 'Insert' &&
      getValue(context, context.pos - 1, context.pos) === ' '
    ) {
      from = context.pos;
    }

    if (nodeBefore.name === 'Identifier') {
      from = nodeBefore.from;
      to = nodeBefore.to;
      isNameSlot = nodeBefore.prevSibling?.name === '{{';

      const idValue = getValue(context, from, to);
      path = idValue.split('.').slice(0, -1);
      if (path.length) {
        from += path.join('.').length + 1;
      }
    }

    let postfix = '';
    if (nodeAfter.name !== '}}') {
      postfix = '}}';
    }

    if (from) {
      let variables = variablesRef.current;

      path.forEach(
        (item) => (variables = variables?.find((i) => i.name === item)?.props)
      );

      const options: Completion[] =
        variables?.map(
          ({ name, value, props }) =>
            ({
              label: name,
              detail: trimDetail(value),
              apply: name + (props ? '.' : postfix),
              type: props ? 'object' : 'variable',
            } satisfies Completion)
        ) || [];

      if (isNameSlot && !path.length) {
        options.push(...(helpersRef?.current ?? []));
      }

      return {
        from: from,
        to,
        options,
      };
    }

    return null;
  };
