import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { BaseView } from 'tg.component/layout/BaseView';
import { WebsocketClient } from '../../websocket-client/WebsocketClient';

export const WebsocketPreview = () => {
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );

  useEffect(() => {
    if (jwtToken) {
      const client = WebsocketClient({
        authentication: { jwtToken: jwtToken },
        serverUrl: process.env.REACT_APP_API_URL,
      });
      client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (data) => addMessage(JSON.stringify(data, undefined, 2))
      );
      return () => client.disconnect();
    }
  }, [config, project, jwtToken]);

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
