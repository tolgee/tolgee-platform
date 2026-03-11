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
  enabled = true,
}: QaPreviewProps): QaPreviewResult => {
  const [issuesByType, setIssuesByType] = useState<
    Map<string, QaPreviewIssue[]>
  >(new Map());
  const [isLoading, setIsLoading] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const pendingTextUpdateRef = useRef(false);
  const jwtToken = useGlobalContext((c) => c.auth.jwtToken);

  useEffect(() => {
    if (!enabled || !jwtToken) return;

    setIssuesByType(new Map());
    setIsLoading(false);

    // in format http(s)://host:port
    const url = new URL(
      import.meta.env.VITE_APP_API_URL || window.location.origin
    );
    const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${url.host}/ws/qa-preview`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      // auth message
      ws.send(
        JSON.stringify({ token: jwtToken, projectId, keyId, languageTag })
      );
      // initial text update message
      if (text !== null && text !== undefined) {
        ws.send(JSON.stringify({ text }));
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

  const sendText = useDebouncedCallback((t: string) => {
    pendingTextUpdateRef.current = false;
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ text: t }));
    }
  }, 300);

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
