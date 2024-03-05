import { Placeholder } from '@tginternal/editor';

type Props = {
  placeholder: Placeholder;
  pluralExampleValue?: number | undefined;
  key: any;
  props?: React.HtmlHTMLAttributes<HTMLDivElement>;
};

export const placeholderToElement = ({
  placeholder,
  pluralExampleValue,
  key,
  props,
}: Props) => {
  const className = `placeholder-widget placeholder-${placeholder.type}`;

  switch (placeholder.type) {
    case 'hash':
      return (
        <div {...props} key={key} className={className}>
          <div>{`${placeholder.name}${pluralExampleValue ?? ''}`}</div>
        </div>
      );
    case 'variable':
      return (
        <div {...props} key={key} className={className}>
          <div>{placeholder.name}</div>
        </div>
      );
    case 'tagOpen':
      return (
        <div {...props} key={key} className={className}>
          <div>{placeholder.name}</div>
        </div>
      );
    case 'tagClose':
      return (
        <div {...props} key={key} className={className}>
          <div>{placeholder.name}</div>
        </div>
      );
  }
};
