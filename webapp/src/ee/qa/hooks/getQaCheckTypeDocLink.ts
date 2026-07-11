import { QaCheckType } from 'tg.service/apiSchemaTypes';
import { DOCS_LINKS } from 'tg.constants/docLinks';

export function getQaCheckTypeDocLink(type: QaCheckType): string | null {
  switch (type) {
    case 'SPELLING':
      return DOCS_LINKS.qaChecksSpelling;
    case 'GRAMMAR':
      return DOCS_LINKS.qaChecksGrammar;
    default:
      return null;
  }
}
