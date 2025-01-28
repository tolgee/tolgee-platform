import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useState } from 'react';
import { BaseView } from 'tg.component/layout/BaseView';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const WebsocketPreview = () => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useGlobalContext((c) => c.auth.jwtToken);
  const client = useGlobalContext((c) => c.wsClient.client);
  const user = useUser();

  useEffect(() => {
    if (client) {
      return client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (data) => addMessage(JSON.stringify(data, undefined, 2))
      );
    }
  }, [config, project, jwtToken, client]);

  useEffect(() => {
    if (client && user) {
      return client.subscribe(
        `/users/${user.id}/notifications-changed`,
        (data) => addMessage(JSON.stringify(data, undefined, 2))
      );
    }
  }, [config, user, jwtToken, client]);

  const [messages, setMessages] = useState([] as string[]);

  const addMessage = (message: string) => {
    setMessages((messages) => [...messages, message]);
  };

  return (
    <BaseView windowTitle="Sockets demo">
      {messages.map((v, idx) => (
        <pre key={idx}>{v}</pre>
      ))}
    </BaseView>
  );
};
