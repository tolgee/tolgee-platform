import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';

import type { EmptyResourceMessage } from 'tg.component/common/EmptyResourceMessage';
import { AddFirstGlossaryMessage } from './AddFirstGlossaryMessage';

vi.mock('tg.component/common/EmptyResourceMessage', () => ({
  EmptyResourceMessage: ({
    message,
    button,
  }: React.ComponentProps<typeof EmptyResourceMessage>) => (
    <>
      <p>{message.defaultValue ?? message.keyName}</p>
      {button?.onClick && <button type="button">{button.keyName}</button>}
    </>
  ),
}));

describe('AddFirstGlossaryMessage', () => {
  it('explains who can help when creation is unavailable', () => {
    const markup = renderToStaticMarkup(<AddFirstGlossaryMessage />);

    expect(markup).toContain('permission to create glossaries');
    expect(markup).toContain('Owner or Maintainer');
    expect(markup).not.toContain('<button');
  });

  it('keeps the getting-started message and button for creators', () => {
    const markup = renderToStaticMarkup(
      <AddFirstGlossaryMessage onCreateClick={vi.fn()} />
    );

    expect(markup).toContain('glossaries_list_empty_message');
    expect(markup).toContain('glossaries_add_first_button</button>');
  });
});
