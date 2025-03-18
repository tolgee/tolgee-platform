import { hoverTooltip } from '@codemirror/view';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';

import { components } from 'tg.service/apiSchema.generated';

type PromptVariable = components['schemas']['PromptVariable'];

export const handlebarsTooltip = (
  variablesRef: RefObject<PromptVariable[] | undefined>
) =>
  hoverTooltip((context, pos, side) => {
    const tree = syntaxTree(context.state);
    const node = tree.resolveInner(pos);
    const variableName = context.state.doc
      .toString()
      .substring(node.from, node.to);

    const variable = variablesRef.current?.find((i) => i.name === variableName);

    if (node.name === 'Identifier') {
      return {
        pos: node.from,
        end: node.to,
        create(view) {
          const dom = document.createElement('div');
          dom.textContent = variable
            ? variable.value || 'Empty'
            : 'Unknown variable';
          return { dom };
        },
      };
    }
    return null;
  });
