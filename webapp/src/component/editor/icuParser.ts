import {
  parse,
  TYPE,
  MessageFormatElement,
  PluralOrSelectOption,
} from '@formatjs/icu-messageformat-parser';

export type ParameterType = {
  name: string;
  function: string | null;
  options: string[];
};

const flatOptions = (options: PluralOrSelectOption[]) => {
  let elements: ParameterType[] = [];
  options.forEach((o) =>
    o.value.forEach((el) => {
      elements = [...elements, ...flatTree(el)];
    })
  );
  return elements;
};

const flatTree = (root: MessageFormatElement): ParameterType[] => {
  switch (root.type) {
    case TYPE.select:
      return [
        {
          name: root.value,
          function: TYPE[root.type],
          options: Object.keys(root.options),
        },
        ...flatOptions(Object.values(root.options)),
      ];
    case TYPE.plural:
      return [
        {
          name: root.value,
          function: root.pluralType === 'cardinal' ? 'plural' : 'selectordinal',
          options: Object.keys(root.options),
        },
        ...flatOptions(Object.values(root.options)),
      ];
    case TYPE.date:
    case TYPE.time:
    case TYPE.number:
    case TYPE.argument:
      return [
        {
          name: root.value,
          function: TYPE[root.type],
          options: [],
        },
      ];
    case TYPE.tag:
      return [
        {
          name: root.value,
          function: TYPE[root.type],
          options: [],
        },
        ...root.children.flatMap(flatTree),
      ];
    default:
      return [];
  }
};

export const getParameters = (text: string) => {
  let final: ParameterType[] = [];
  const elements = parse(text, { ignoreTag: true });
  for (const element of elements) {
    final = [...final, ...flatTree(element)];
  }
  return final;
};
