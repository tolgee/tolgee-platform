import { hoverTooltip } from '@codemirror/view';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';

import { components } from 'tg.service/apiSchema.generated';

type PromptVariableDto = components['schemas']['PromptVariableDto'];

export const handlebarsTooltip = (
  variablesRef: RefObject<PromptVariableDto[] | undefined>,
  unknownVariableMessageRef?: RefObject<string | undefined>
) =>
  hoverTooltip((context, pos, side) => {
    const tree = syntaxTree(context.state);
    const node = tree.resolveInner(pos);

    if (node.name === 'Identifier') {
      const variableName = context.state.doc
        .toString()
        .substring(node.from, node.to);
      const path = variableName.split('.');

      let variable: PromptVariableDto | undefined = {
        name: '',
        props: variablesRef.current,
      };
      path.forEach(
        (item) => (variable = variable?.props?.find((i) => i.name === item))
      );
      return {
        pos: node.from,
        end: node.to,
        create() {
          const dom = document.createElement('div');
          const text = document.createElement('div');
          text.textContent = variable
            ? (variable.description ?? variable.value) || 'Empty'
            : unknownVariableMessageRef?.current ?? 'Unknown variable';
          dom.appendChild(text);

          let startNode = node.prevSibling;
          while (startNode && startNode?.name !== '{{') {
            startNode = startNode.prevSibling;
          }
          let endNode = node.nextSibling;
          while (endNode && endNode?.name !== '}}') {
            endNode = endNode.nextSibling;
          }

          if (variable?.value && startNode && endNode) {
            const insertText = variable.value;
            const onExpand = () => {
              const transaction = context.state.update({
                changes: {
                  from: startNode.from,
                  to: endNode.to,
                  insert: insertText,
                },
              });
              context.dispatch(transaction);
            };

            const button = document.createElement('button');
            button.textContent = 'Expand';
            button.addEventListener('click', onExpand);
            dom.appendChild(button);
          }
          return { dom };
        },
      };
    }
    return null;
  });
