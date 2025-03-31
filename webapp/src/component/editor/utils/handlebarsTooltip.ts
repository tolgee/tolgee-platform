import { hoverTooltip } from '@codemirror/view';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { trimDetail } from './handlebarsAutocomplete';

type PromptVariableDto = components['schemas']['PromptVariableDto'];

export const handlebarsTooltip = (
  variablesRef: RefObject<PromptVariableDto[] | undefined>,
  unknownVariableMessageRef?: RefObject<string | undefined>
) =>
  hoverTooltip((context, pos, side) => {
    const tree = syntaxTree(context.state);
    const node = tree.resolveInner(pos);
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

    if (node.name === 'Identifier' && variable) {
      return {
        pos: node.from,
        end: node.to,
        create() {
          const dom = document.createElement('div');
          dom.style.whiteSpace = 'nowrap';
          dom.textContent = variable
            ? trimDetail(variable.description ?? variable.value, 50) || 'Empty'
            : unknownVariableMessageRef?.current ?? 'Unknown variable';
          return { dom };
        },
      };
    }
    return null;
  });
