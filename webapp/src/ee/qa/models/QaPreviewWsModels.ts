import { QaIssueModel } from 'tg.service/apiSchemaTypes.generated';

export type QaPreviewIssue = Omit<QaIssueModel, 'id'>;

export type WsResultMessage = {
  type: 'result';
  checkType: string;
  issues: QaPreviewIssue[];
};

export type WsDoneMessage = {
  type: 'done';
};

export type WsErrorMessage = {
  type: 'error';
  message: string;
};

export type WsMessage = WsResultMessage | WsDoneMessage | WsErrorMessage;

export type QaPreviewProps = {
  projectId: number;
  keyId: number;
  languageTag: string;
  text: string | undefined | null;
  variant?: string;
  enabled?: boolean;
  initialIssues?: QaPreviewIssue[];
};

export type QaPreviewResult = {
  issues: QaPreviewIssue[];
  isLoading: boolean;
  isDisconnected: boolean;
  updateIssueState: (
    issue: QaPreviewIssue,
    newState: QaPreviewIssue['state']
  ) => void;
};
