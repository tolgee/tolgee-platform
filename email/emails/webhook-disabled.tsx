import * as React from 'react';
import { Text } from '@react-email/components';
import t from '../components/translate';
import ClassicLayout from '../components/layouts/ClassicLayout';
import LocalizedText from '../components/LocalizedText';
import If from '../components/If';

export default function WebhookDisabledEmail() {
  return (
    <ClassicLayout
      subject={t.raw(
        'webhook-disabled-subject',
        'Webhook automatically disabled'
      )}
      sendReason={t.raw(
        'webhook-disabled-send-reason',
        "You're receiving this email because you're an owner of the organization {organizationName} on {instanceQualifier}",
        { organizationName: 'My Organization', instanceQualifier: 'Tolgee' }
      )}
    >
      <Text style={{ margin: '0 0 16px' }}>
        <If condition="${recipientName}">
          <If.Then>
            <LocalizedText
              keyName="webhook-disabled-greetings"
              defaultValue="Hello {recipientName},"
              demoParams={{ recipientName: 'Bob' }}
            />
          </If.Then>
          <If.Else>
            <LocalizedText
              keyName="email-general-greetings"
              defaultValue="Hello! 👋,"
            />
          </If.Else>
        </If>
      </Text>
      <Text>
        <LocalizedText
          keyName="webhook-disabled-body"
          defaultValue="The webhook {webhookUrl} in project {projectName} has been automatically disabled after failing continuously for more than 3 days."
          demoParams={{
            webhookUrl: 'https://example.com/webhook',
            projectName: 'My Project',
          }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="webhook-disabled-action"
          defaultValue="You can re-enable it from the project's Developer settings in the Webhooks section."
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="email-signature"
          defaultValue={'Kind Regards,\nTolgee'}
        />
      </Text>
    </ClassicLayout>
  );
}
