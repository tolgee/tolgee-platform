import * as React from 'react';
import { Text } from '@react-email/components';
import t from '../components/translate';
import ClassicLayout from '../components/layouts/ClassicLayout';
import LocalizedText from '../components/LocalizedText';
import If from '../components/If';

export default function AppManifestUnhealthyEmail() {
  return (
    <ClassicLayout
      subject={t.raw(
        'app-manifest-unhealthy-subject',
        'A Tolgee app you own is unreachable'
      )}
      sendReason={t.raw(
        'app-manifest-unhealthy-send-reason',
        "You're receiving this email because you're an owner of the organization {organizationName} on {instanceQualifier}",
        { organizationName: 'My Organization', instanceQualifier: 'Tolgee' }
      )}
    >
      <Text style={{ margin: '0 0 16px' }}>
        <If condition="${recipientName}">
          <If.Then>
            <LocalizedText
              keyName="app-manifest-unhealthy-greetings"
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
          keyName="app-manifest-unhealthy-body"
          defaultValue="The manifest of your app {appName} at {manifestUrl} has been failing continuously. The last error was: {lastError}"
          demoParams={{
            appName: 'My App',
            manifestUrl: 'https://example.com/manifest.json',
            lastError: 'connection refused',
          }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="app-manifest-unhealthy-action"
          defaultValue="The app keeps working for now. Please make the manifest reachable again — if it stays unreachable, the app may be removed from every organization that installed it after {removeAfterDays} days."
          demoParams={{ removeAfterDays: '14' }}
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
