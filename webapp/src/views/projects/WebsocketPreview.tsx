import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { BaseView } from 'tg.component/layout/BaseView';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const WebsocketPreview = () => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );
  const client = useGlobalContext((c) => c.client);

  useEffect(() => {
    if (jwtToken && client) {
      return client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (data) => addMessage(JSON.stringify(data, undefined, 2))
      );
    }
  }, [config, project, jwtToken, client]);

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
