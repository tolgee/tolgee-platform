import { useEffect, useMemo, useRef, useState } from 'react';
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
  const wsRef = useRef<WebSocket | null>(null);
  const pendingTextUpdateRef = useRef(false);
  const jwtToken = useGlobalContext((c) => c.auth.jwtToken);
  const initialIssuesRef = useRef(initialIssues);
  initialIssuesRef.current = initialIssues;

  useEffect(() => {
    // Re-seed from persisted issues when params change
    setIssuesByType(groupIssuesByType(initialIssuesRef.current));
    setIsLoading(false);

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
      setIsLoading(false);
      // auth message
      ws.send(
        JSON.stringify({ token: jwtToken, projectId, keyId, languageTag })
      );
      // initial text update message
      if (text !== null && text !== undefined) {
        ws.send(JSON.stringify({ text, variant }));
        setIsLoading(true);
      }
    };

    ws.onmessage = (event) => {
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
      }
    };

    ws.onerror = () => {
      setIsLoading(false);
    };

    ws.onclose = () => {
      wsRef.current = null;
    };

    return () => {
      ws.close();
      wsRef.current = null;
    };
  }, [projectId, keyId, languageTag, enabled, jwtToken]);

  const variantRef = useRef(variant);
  variantRef.current = variant;

  const sendText = useDebouncedCallback(
    (t: string) => {
      pendingTextUpdateRef.current = false;
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(
          JSON.stringify({ text: t, variant: variantRef.current })
        );
      }
    },
    200,
    { maxWait: 1000 }
  );

  useEffect(() => {
    if (text !== null && text !== undefined && enabled) {
      setIsLoading(true);
      pendingTextUpdateRef.current = true;
      sendText(text);
    }
  }, [text, enabled]);

  const issues = useMemo(() => {
    const all = Array.from(issuesByType.values()).flat();
    return all.sort(
      (a, b) =>
        a.positionStart - b.positionStart ||
        a.positionEnd - b.positionEnd ||
        a.type.localeCompare(b.type)
    );
  }, [issuesByType]);

  return { issues, isLoading };
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
