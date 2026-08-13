import API from '@openreplay/tracker';

declare module '*.svg' {
  const content: React.FunctionComponent<
    React.PropsWithChildren<React.SVGAttributes<SVGElement>>
  >;
  export default content;
}

declare global {
  interface Window {
    openReplayTracker?: API;
  }
}

declare module 'react' {
  interface HTMLAttributes<T> extends AriaAttributes, DOMAttributes<T> {
    webkitdirectory?: boolean;
  }

  type KeyOf<T> = {
    [K in keyof T]-?: T[K] extends Key ? K : never;
  }[keyof T];
}
