import { TranslationsClient } from '@tolgee/socketio-client';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { BaseView } from 'tg.component/layout/BaseView';

const events = [
  'connect_error',
  'connect',
  'translation_created',
  'translation_modified',
  'translation_deleted',
  'key_created',
  'key_modified',
  'key_deleted',
  'reconnect_attempt',
] as const;

export const SocketIoPreview = () => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );
  const [messages, setMessages] = useState([] as string[]);

  const client = useMemo(() => {
    return new TranslationsClient({
      transports: config.socket.allowedTransports as any,
      serverUrl:
        config.socket.serverUrl ||
        `${window.location.protocol}//${window.location.host}:9090`,
      authentication: {
        projectId: project.id,
        jwtToken: jwtToken!,
      },
    });
  }, [config, project, jwtToken]);

  const addMessage = (message: string) => {
    setMessages((messages) => [...messages, message]);
  };

  useEffect(() => {
    const subscriptions = events.map((e) =>
      client.on(e, (data) => {
        addMessage(e + ':');
        addMessage(JSON.stringify(data, undefined, 2));
        addMessage('----------');
      })
    );
    return () => subscriptions.forEach((unsubscribe) => unsubscribe());
  });

  return (
    <BaseView windowTitle="Sockets demo">
      {messages.map((v, idx) => (
        <pre key={idx}>{v}</pre>
      ))}
    </BaseView>
  );
};
