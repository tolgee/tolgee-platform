import { useEffect, useState } from 'react';
import { WebsocketClient } from 'tg.websocket-client/WebsocketClient';

export const useWebsocketService = (jwtToken: string | undefined) => {
  const [client, setClient] = useState<ReturnType<typeof WebsocketClient>>();
  const [clientConnected, setClientConnected] = useState<boolean>();

  useEffect(() => {
    if (jwtToken) {
      const newClient = WebsocketClient({
        authentication: { jwtToken: jwtToken },
        serverUrl: import.meta.env.VITE_APP_API_URL,
        onConnected: () => setClientConnected(true),
        onConnectionClose: () => setClientConnected(false),
      });
      setClient(newClient);
      return () => {
        newClient.disconnect();
      };
    }
  }, [jwtToken]);

  return {
    client,
    clientConnected,
  };
};
