import { EditorView } from 'codemirror';
import { PluralEditor } from './translationVisual/PluralEditor';
import { useTranslationCell } from './useTranslationCell';

type Props = {
  mode: 'placeholders' | 'syntax';
  tools: ReturnType<typeof useTranslationCell>;
  editorRef: React.RefObject<EditorView>;
};

export const TranslationEditor = ({ mode, tools, editorRef }: Props) => {
  const {
    editVal,
    language,
    setState,
    setVariant,
    setEditValue,
    handleSave,
    handleClose,
    handleInsertBase,
    baseValue,
  } = tools;

  return (
    <PluralEditor
      locale={language.tag}
      value={editVal!.value}
      onChange={setEditValue}
      activeVariant={editVal!.activeVariant}
      onActiveVariantChange={setVariant}
      autofocus={true}
      activeEditorRef={editorRef}
      mode={mode}
      baseValue={baseValue}
      editorProps={{
        shortcuts: [
          { key: 'Escape', run: () => (handleClose(true), true) },
          { key: `Mod-e`, run: () => (setState(), true) },
          {
            key: 'Mod-Enter',
            run: () => (handleSave({ after: 'EDIT_NEXT' }), true),
          },
          { key: 'Enter', run: () => (handleSave({}), true) },
          {
            key: 'Mod-Insert',
            mac: 'Cmd-Shift-s',
            run: () => (!language.base && handleInsertBase(), true),
          },
        ],
      }}
    />
  );
};
