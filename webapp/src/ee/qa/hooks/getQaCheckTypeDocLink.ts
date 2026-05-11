import { components } from 'tg.service/apiSchema.generated';
import { DOCS_LINKS } from 'tg.constants/docLinks';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

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
