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
          dom.textContent = variable
            ? (variable.description ?? variable.value) || 'Empty'
            : unknownVariableMessageRef?.current ?? 'Unknown variable';
          return { dom };
        },
      };
    }
    return null;
  });
