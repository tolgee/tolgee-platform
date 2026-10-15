import { act, StrictMode } from 'react';
import { createRoot, Root } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ReactList } from './ReactList';

describe('ReactList', () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    // @ts-ignore
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(() => {
    act(() => root.unmount());
    container.remove();
    vi.restoreAllMocks();
  });

  it('keeps listening to scroll after a StrictMode remount', () => {
    const listeners = new Set<EventListenerOrEventListenerObject>();
    const add = window.addEventListener.bind(window);
    const remove = window.removeEventListener.bind(window);
    vi.spyOn(window, 'addEventListener').mockImplementation(
      (type, listener, options) => {
        if (type === 'scroll') listeners.add(listener);
        add(type, listener, options);
      }
    );
    vi.spyOn(window, 'removeEventListener').mockImplementation(
      (type, listener, options) => {
        if (type === 'scroll') listeners.delete(listener);
        remove(type, listener, options);
      }
    );

    act(() =>
      root.render(
        <StrictMode>
          <ReactList
            type="variable"
            length={20}
            // @ts-ignore
            scrollParentGetter={() => window}
            itemRenderer={(index) => <div key={index}>{index}</div>}
          />
        </StrictMode>
      )
    );

    expect(listeners.size).toBe(1);
  });
});
