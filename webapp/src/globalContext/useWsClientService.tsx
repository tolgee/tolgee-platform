import { useEffect, useState } from 'react';
import { WebsocketClient } from 'tg.websocket-client/WebsocketClient';

export const useWebsocketService = (
  jwtToken: string | undefined,
  allowPrivate: boolean
) => {
  const [client, setClient] = useState<ReturnType<typeof WebsocketClient>>();
  const [clientConnected, setClientConnected] = useState<boolean>();

  useEffect(() => {
    if (allowPrivate) {
      const newClient = WebsocketClient({
        authentication: { jwtToken: jwtToken! },
        serverUrl: import.meta.env.VITE_APP_API_URL,
        onConnected: () => setClientConnected(true),
        onConnectionClose: () => setClientConnected(false),
      });
      setClient(newClient);
      return () => {
        newClient.deactivate();
      };
    }
  }, [jwtToken, allowPrivate]);

  return {
    client,
    clientConnected,
  };
};
