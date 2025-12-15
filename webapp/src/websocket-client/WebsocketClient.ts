import { CompatClient, Stomp } from '@stomp/stompjs'; // @ts-ignore
import SockJS from 'sockjs-client/dist/sockjs';
import { components } from 'tg.service/apiSchema.generated';

type BatchJobModelStatus = components['schemas']['BatchJobModel']['status'];

type WebsocketClientOptions = {
  serverUrl?: string;
  authentication: {
    jwtToken: string;
  };
  onConnected?: () => void;
  onError?: () => void;
  onConnectionClose?: () => void;
};

type Message = {
  type: string;
  actor: any;
  data: any;
};

type Subscription<T extends string> = {
  id?: string;
  channel: T;
  callback: (data: any) => void;
  unsubscribe?: () => void;
};

export const WebsocketClient = (options: WebsocketClientOptions) => {
  options.serverUrl = options.serverUrl || window.origin;

  let _client: CompatClient | undefined;
  let _deactivated = false;
  let connected = false;
  let subscriptions: Subscription<any>[] = [];

  const resubscribe = () => {
    if (_deactivated) {
      return;
    }

    if (_client) {
      subscriptions.forEach((subscription) => {
        subscribeToStompChannel(subscription);
      });
    }
  };

  const subscribeToStompChannel = (subscription: Subscription<any>) => {
    if (connected) {
      const stompSubscription = _client!.subscribe(
        subscription.channel,
        function (message) {
          const parsed = JSON.parse(message.body) as Message;
          subscription.callback(parsed as any);
        }
      );
      subscription.unsubscribe = stompSubscription.unsubscribe;
      subscription.id = stompSubscription.id;
    }
  };

  function initClient() {
    _client = Stomp.over(() => new SockJS(`${options.serverUrl}/websocket`));
    _client.configure({
      reconnectDelay: 3000,
      debug: (msg) => {},
    });
  }

  function connectIfNotAlready() {
    if (_deactivated) {
      return;
    }

    const client = getClient();

    const onConnected = function () {
      connected = true;
      resubscribe();
      options.onConnected?.();
    };

    const onDisconnect = function () {
      connected = false;
      subscriptions.forEach((s) => {
        s.unsubscribe = undefined;
        s.id = undefined;
        removeSubscription(s);
      });
      options.onConnectionClose?.();
    };

    const onError = () => {
      options.onError?.();
    };

    const headers: Record<string, string> | null = {
      jwtToken: options.authentication.jwtToken,
      Authorization: `Bearer ${options.authentication.jwtToken}`,
    };

    client.connect(headers, onConnected, onError, onDisconnect);
  }

  const getClient = () => {
    if (_client !== undefined) {
      return _client;
    }
    initClient();
    return _client!;
  };

  /**
   * Subscribes to channel
   * @param channel Channel URI
   * @param callback Callback function to be executed when event is triggered
   * @return Function Function unsubscribing the event listening
   */
  function subscribe<T extends ChannelProject | ChannelUser>(
    channel: T,
    callback: (data: Data<T>) => void
  ): () => void {
    if (_deactivated) {
      return () => {};
    }

    connectIfNotAlready();
    const subscription: Subscription<any> = { channel, callback };
    subscriptions.push(subscription);
    subscribeToStompChannel(subscription);

    return () => {
      subscription.unsubscribe?.();
      removeSubscription(subscription);
    };
  }

  function disconnect() {
    if (_client) {
      _client.disconnect();
    }
  }

  function deactivate() {
    _deactivated = true;
    disconnect();
  }

  function removeSubscription(subscription: Subscription<any>) {
    subscriptions = subscriptions.filter((it) => it !== subscription);
  }

  return Object.freeze({ subscribe, deactivate });
};

export type EventTypeProject =
  | 'translation-data-modified'
  | 'batch-job-progress';
export type ChannelProject = `/projects/${number}/${EventTypeProject}`;

export type EventTypeUser = 'notifications-changed';
export type ChannelUser = `/users/${number}/${EventTypeUser}`;

export type TranslationsModifiedData = WebsocketEvent<{
  translations: EntityModification<'translation'>[] | null;
  keys: EntityModification<'key'>[] | null;
}>;

export type BatchJobProgress = WebsocketEvent<{
  jobId: number;
  processed: number;
  status: BatchJobModelStatus;
  total: number;
  errorMessage: string | undefined;
}>;

export type NotificationsChanged = WebsocketEvent<{
  currentlyUnseenCount: number;
  newNotification?: components['schemas']['NotificationModel'];
}>;

export type EntityModification<T> = T extends keyof schemas
  ? {
      id: number;
      modifications: Partial<schemas[T]['mutableFields']>;
      relations: schemas[T]['relations'];
      changeType: 'MOD' | 'DEL' | 'ADD';
    }
  : never;

export type WebsocketEvent<Data> = {
  activityId: number;
  actor: { type: 'user'; data: components['schemas']['UserAccountModel'] };
  data: Data;
};

interface schemas extends Record<string, SchemaDefinition> {
  key: {
    description: {
      name: string;
    };
    mutableFields: {
      name: Modification<string>;
    };
  };
  translation: {
    description: {
      text: string;
    };
    mutableFields: {
      text: Modification<string>;
      state: Modification<components['schemas']['TranslationModel']['state']>;
    };
    relations: {
      key: Relation<schemas['key']['description']>;
      language: Relation<schemas['language']['description']>;
    };
  };
  language: {
    description: {
      name: string;
      tag: string;
    };
  };
}

type Relation<T> = {
  entityId: number;
  data: T;
  relations: Record<string, Relation<any>>;
};

type SchemaDefinition = {
  description: Record<string, any>;
  mutableFields?: Record<string, Modification<any>>;
  relations?: Record<string, Relation<any>>;
};

export type Modification<T> = { old: T; new: T };

export type Data<T> = T extends `/projects/${number}/translation-data-modified`
  ? TranslationsModifiedData
  : T extends `/projects/${number}/batch-job-progress`
  ? BatchJobProgress
  : T extends `/users/${number}/notifications-changed`
  ? NotificationsChanged
  : never;
