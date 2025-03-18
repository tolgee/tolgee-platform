import { CompletionSource } from '@codemirror/autocomplete';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';
import { components } from 'tg.service/apiSchema.generated';

type PromptVariable = components['schemas']['PromptVariable'];

function trimDetail(text, maxLength = 20) {
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

export const handlebarsAutocomplete =
  (variablesRef: RefObject<PromptVariable[] | undefined>): CompletionSource =>
  (context) => {
    const nodeBefore = syntaxTree(context.state).resolveInner(context.pos, -1);

    if (nodeBefore.name === '{{') {
      return {
        from: context.pos,
        options:
          variablesRef.current?.map(({ name, value }) => ({
            label: name,
            detail: trimDetail(value),
          })) || [],
      };
    }

    if (nodeBefore.name === 'Identifier') {
      return {
        from: nodeBefore.from,
        options:
          variablesRef.current?.map(({ name, value }) => ({
            label: name,
            detail: trimDetail(value),
          })) || [],
      };
    }

    return null;
  };
