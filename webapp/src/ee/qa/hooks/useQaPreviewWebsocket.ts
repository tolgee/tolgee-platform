import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDebouncedCallback } from 'use-debounce';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import {
  QaPreviewIssue,
  QaPreviewProps,
  QaPreviewResult,
  WsMessage,
} from 'tg.ee.module/qa/models/QaPreviewWsModels';

export const useQaPreviewWebsocket = ({
  projectId,
  keyId,
  languageTag,
  text,
  variant,
  enabled = true,
  initialIssues,
}: QaPreviewProps): QaPreviewResult => {
  const [issuesByType, setIssuesByType] = useState<
    Map<string, QaPreviewIssue[]>
  >(() => groupIssuesByType(initialIssues));
  const [isLoading, setIsLoading] = useState(false);
  const [isDisconnected, setIsDisconnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const pendingTextUpdateRef = useRef(false);
  const jwtToken = useGlobalContext((c) => c.auth.jwtToken);
  const initialIssuesRef = useRef(initialIssues);
  initialIssuesRef.current = initialIssues;
  const latestTextRef = useRef(text);
  latestTextRef.current = text;
  const latestVariantRef = useRef(variant);
  latestVariantRef.current = variant;

  useEffect(() => {
    // Re-seed from persisted issues when params change
    setIssuesByType(groupIssuesByType(initialIssuesRef.current));
    setIsLoading(false);
    setIsDisconnected(false);

    if (!enabled || !jwtToken) return;

    // in format http(s)://host:port
    const url = new URL(
      import.meta.env.VITE_APP_API_URL || window.location.origin
    );
    const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${url.host}/ws/qa-preview`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    setIsLoading(true);
    ws.onopen = () => {
      // auth message
      ws.send(
        JSON.stringify({ token: jwtToken, projectId, keyId, languageTag })
      );
      // initial text update message
      const currentText = latestTextRef.current;
      if (currentText != null) {
        ws.send(
          JSON.stringify({
            text: currentText,
            variant: latestVariantRef.current,
          })
        );
      } else {
        setIsLoading(false);
      }
    };

    ws.onmessage = (event) => {
      try {
        const data: WsMessage = JSON.parse(event.data);
        if (data.type === 'result') {
          setIssuesByType((prev) => {
            const next = new Map(prev);
            next.set(data.checkType, data.issues);
            return next;
          });
        } else if (data.type === 'done') {
          if (!pendingTextUpdateRef.current) {
            setIsLoading(false);
          }
        } else if (data.type === 'error') {
          // eslint-disable-next-line no-console
          console.error('QA preview websocket error:', data.message);
          setIsLoading(false);
        }
      } catch (error) {
        // Malformed message from server — ignore
        // eslint-disable-next-line no-console
        console.error(
          'Malformed QA preview websocket message:',
          event.data,
          error
        );
      }
    };

    ws.onerror = () => {
      setIsLoading(false);
    };

    ws.onclose = () => {
      if (wsRef.current === ws) {
        setIsDisconnected(true);
        wsRef.current = null;
      }
    };

    return () => {
      sendText.cancel();
      pendingTextUpdateRef.current = false;
      ws.close();
      if (wsRef.current === ws) {
        wsRef.current = null;
      }
    };
  }, [projectId, keyId, languageTag, enabled, jwtToken]);

  // Sync from persisted issues when preview WS is not active
  useEffect(() => {
    if (!enabled) {
      setIssuesByType(groupIssuesByType(initialIssues));
    }
  }, [initialIssues, enabled]);

  const sendText = useDebouncedCallback(
    (t: string) => {
      pendingTextUpdateRef.current = false;
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(
          JSON.stringify({ text: t, variant: latestVariantRef.current })
        );
      }
    },
    200,
    { maxWait: 1000 }
  );

  // variant is read from variantRef inside sendText, so it doesn't need to be
  // a dependency — variant changes always accompany text changes for plurals.
  useEffect(() => {
    if (text != null && enabled && wsRef.current) {
      setIsLoading(true);
      pendingTextUpdateRef.current = true;
      sendText(text);
    }
  }, [text, enabled]);

  const updateIssueState = useCallback(
    (targetIssue: QaPreviewIssue, newState: string) => {
      setIssuesByType((prev) => {
        const next = new Map(prev);
        const typeIssues = next.get(targetIssue.type);
        if (typeIssues) {
          next.set(
            targetIssue.type,
            typeIssues.map((i) =>
              i.message === targetIssue.message &&
              i.replacement === targetIssue.replacement &&
              i.positionStart === targetIssue.positionStart &&
              i.positionEnd === targetIssue.positionEnd &&
              i.pluralVariant === targetIssue.pluralVariant
                ? { ...i, state: newState as QaPreviewIssue['state'] }
                : i
            )
          );
        }
        return next;
      });
    },
    []
  );

  const issues = useMemo(() => {
    const all = Array.from(issuesByType.values()).flat();
    return all.sort(
      (a, b) =>
        (a.positionStart ?? Infinity) - (b.positionStart ?? Infinity) ||
        (a.positionEnd ?? Infinity) - (b.positionEnd ?? Infinity) ||
        a.type.localeCompare(b.type)
    );
  }, [issuesByType]);

  return { issues, isLoading, isDisconnected, updateIssueState };
};

const groupIssuesByType = (
  issues: QaPreviewIssue[] | undefined
): Map<string, QaPreviewIssue[]> => {
  const map = new Map<string, QaPreviewIssue[]>();
  for (const issue of issues ?? []) {
    const existing = map.get(issue.type);
    if (existing) {
      existing.push(issue);
    } else {
      map.set(issue.type, [issue]);
    }
  }
  return map;
};
