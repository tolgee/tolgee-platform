import { T } from '@tolgee/react';
import { confirmation } from 'tg.hooks/confirmation';

export function confirmProjectDisconnect(
  projectName: string,
  onConfirm: () => void
) {
  confirmation({
    title: (
      <T
        keyName="tm_settings_disconnect_project_title"
        defaultValue="Disconnect {projectName}"
        params={{ projectName }}
      />
    ),
    message: (
      <T
        keyName="tm_settings_remove_project_message"
        defaultValue="This project will be disconnected from the translation memory."
      />
    ),
    onConfirm,
  });
}
