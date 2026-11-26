import { CopyTranslationButton } from 'tg.views/projects/translations/cell/CopyTranslationButton';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';

const COMPACT_BUTTON_STYLE = {
  width: 28,
  height: 28,
  margin: '-2px -4px -4px',
};

type Props = {
  variant: string;
  content: string;
};

export const CopyVariantButton = ({ variant, content }: Props) => (
  <CopyTranslationButton
    dataCy="translations-cell-copy-variant-button"
    text={content}
    variant={variant}
    className={CELL_SHOW_ON_HOVER}
    style={COMPACT_BUTTON_STYLE}
  />
);
