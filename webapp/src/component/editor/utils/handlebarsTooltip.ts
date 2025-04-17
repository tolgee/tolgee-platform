import { hoverTooltip } from '@codemirror/view';
import { syntaxTree } from '@codemirror/language';
import { RefObject } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';

type PromptVariableDto = components['schemas']['PromptVariableDto'];

export const handlebarsTooltip = (
  variablesRef: RefObject<PromptVariableDto[] | undefined>,
  unknownVariableMessageRef: RefObject<string | undefined>,
  t: ReturnType<typeof useTranslate>['t']
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
        type: 'OBJECT',
      };
      path.forEach(
        (item) => (variable = variable?.props?.find((i) => i.name === item))
      );
      return {
        pos: node.from,
        end: node.to,
        above: false,
        strictSide: true,
        create() {
          const dom = document.createElement('div');

          const title =
            variable?.type === 'FRAGMENT'
              ? t('handlebars_editor_fragment_title')
              : variable?.description;
          const content = variable
            ? variable.value === ''
              ? t('handlebars_editor_variable_empty')
              : variable.value
            : unknownVariableMessageRef?.current ??
              t('handlebars_editor_variable_unknown');

          let onInsert: (() => void) | undefined = undefined;

          if (variable?.value && variable.type === 'FRAGMENT') {
            // find expression boundaries `{{` and `}}`
            let startNode = node.prevSibling;
            while (startNode && startNode?.name !== '{{') {
              startNode = startNode.prevSibling;
            }
            let endNode = node.nextSibling;
            while (endNode && endNode?.name !== '}}') {
              endNode = endNode.nextSibling;
            }
            if (startNode && endNode) {
              const insertText = variable.value;
              onInsert = () => {
                const transaction = context.state.update({
                  changes: {
                    from: startNode.from,
                    to: endNode.to,
                    insert: insertText,
                  },
                });
                context.dispatch(transaction);
              };
            }
          }

          if (title || onInsert) {
            const headerEl = document.createElement('div');
            headerEl.classList.add('header');

            const titleEl = document.createElement('div');
            titleEl.classList.add('title');
            titleEl.textContent = title ?? '';
            headerEl.appendChild(titleEl);

            if (onInsert) {
              const actionEl = document.createElement('div');
              actionEl.classList.add('action');
              actionEl.role = 'button';
              actionEl.textContent = t('handlebars_editor_insert_fragment');
              actionEl.onclick = onInsert;
              headerEl.appendChild(actionEl);
            }

            dom.appendChild(headerEl);
          }

          if (content) {
            const contentEl = document.createElement('div');
            contentEl.classList.add('content');
            contentEl.textContent = content;
            dom.appendChild(contentEl);
          }

          return {
            dom,
          };
        },
      };
    }
    return null;
  });
