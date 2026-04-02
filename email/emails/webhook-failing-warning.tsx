import * as React from 'react';
import { Text } from '@react-email/components';
import t from '../components/translate';
import ClassicLayout from '../components/layouts/ClassicLayout';
import LocalizedText from '../components/LocalizedText';
import If from '../components/If';

export default function WebhookFailingWarningEmail() {
  return (
    <ClassicLayout
      subject={t.raw(
        'webhook-failing-warning-subject',
        'Webhook is failing'
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
          keyName="webhook-failing-warning-body"
          defaultValue="The webhook {webhookUrl} in project {projectName} has been failing continuously. If the issue is not resolved, the webhook will be automatically disabled in {autoDisableAfterDays} days."
          demoParams={{
            webhookUrl: 'https://example.com/webhook',
            projectName: 'My Project',
            autoDisableAfterDays: '3',
          }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="webhook-failing-warning-action"
          defaultValue="Please check the webhook endpoint and ensure it is reachable and responding with a successful status code."
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
