import { CompletionResult, CompletionSource } from '@codemirror/autocomplete';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';
import { components } from 'tg.service/apiSchema.generated';

type PromptVariable = components['schemas']['PromptVariable'];

export function trimDetail(text, maxLength = 20) {
  return text.length > maxLength ? `${text.slice(0, maxLength - 3)}...` : text;
}

export const handlebarsAutocomplete =
  (variablesRef: RefObject<PromptVariable[] | undefined>): CompletionSource =>
  (context) => {
    const tree = syntaxTree(context.state);
    const nodeBefore = tree.resolveInner(context.pos, -1);
    const nodeAfter = tree.resolveInner(context.pos, 1);

    let from: CompletionResult['from'] | undefined = undefined;
    let to: CompletionResult['to'] = undefined;

    if (nodeBefore.name === '{{') {
      from = context.pos;
    }

    if (nodeBefore.name === 'Identifier') {
      from = nodeBefore.from;
      to = nodeBefore.to;
    }

    let postfix = '';
    if (nodeAfter.name === '}}') {
      to = nodeAfter.to;
    } else {
      postfix = '}}';
    }

    if (from) {
      return {
        from,
        to,
        options:
          variablesRef.current?.map(({ name, value }) => ({
            label: name,
            detail: trimDetail(value),
            apply: name + postfix,
          })) || [],
      };
    }

    return null;
  };
