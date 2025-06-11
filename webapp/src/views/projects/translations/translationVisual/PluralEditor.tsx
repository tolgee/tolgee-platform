import { TranslationPlurals } from './TranslationPlurals';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { Editor, EditorProps } from 'tg.component/editor/Editor';
import { TolgeeFormat } from '@tginternal/editor';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { RefObject } from 'react';
import { EditorView } from 'codemirror';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  locale: string;
  value: TolgeeFormat;
  onChange?: (value: TolgeeFormat) => void;
  activeVariant?: string;
  onActiveVariantChange?: (variant: string) => void;
  editorProps?: Partial<EditorProps>;
  autofocus?: boolean;
  activeEditorRef?: RefObject<EditorView | null>;
  mode: 'placeholders' | 'syntax';
  baseValue?: TolgeeFormat;
};

export const PluralEditor = ({
  locale,
  value,
  onChange,
  activeVariant,
  onActiveVariantChange,
  autofocus,
  activeEditorRef,
  editorProps,
  mode,
  baseValue,
}: Props) => {
  function handleChange(text: string, variant: string) {
    onChange?.({ ...value, variants: { ...value.variants, [variant]: text } });
  }

  const project = useProject();

  const editorMode = project.icuPlaceholders ? mode : 'plain';

  function getExactForms() {
    if (!baseValue) {
      return [];
    }
    return Object.keys(baseValue.variants)
      .filter((key) => /^=\d+(\.\d+)?$/.test(key))
      .map((key) => parseFloat(key.substring(1)));
  }

  const exactForms = getExactForms();

  return (
    <TranslationPlurals
      value={value}
      locale={locale}
      showEmpty
      activeVariant={activeVariant}
      variantPaddingTop="8px"
      exactForms={exactForms}
      render={({ content, variant, exampleValue }) => {
        const variantOrOther = variant || 'other';
        return (
          <EditorWrapper data-cy="translation-editor" data-cy-variant={variant}>
            <Editor
              mode={editorMode}
              value={content || ''}
              onChange={(value) => handleChange(value, variantOrOther)}
              onFocus={() => onActiveVariantChange?.(variantOrOther)}
              direction={getLanguageDirection(locale)}
              autofocus={variantOrOther === activeVariant ? autofocus : false}
              minHeight={value.parameter ? 'unset' : '100px'}
              locale={locale}
              editorRef={
                variantOrOther === activeVariant ? activeEditorRef : undefined
              }
              examplePluralNum={exampleValue}
              nested={Boolean(variant)}
              {...editorProps}
            />
          </EditorWrapper>
        );
      }}
    />
  );
};
