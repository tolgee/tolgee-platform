/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import { Section, Text } from '@react-email/components';
import t from '../components/translate';
import ClassicLayout from '../components/layouts/ClassicLayout';
import LocalizedText from '../components/LocalizedText';
import Var from '../components/Var';
import If from '../components/If';

export default function DefaultEmail() {
  return (
    <ClassicLayout
      subject=""
      header={<Var variable="header" demoValue="" />}
      sendReason={t.raw(
        'send-reason-created-account',
        "You're receiving this email because you've created an account on {instanceQualifier}",
        { instanceQualifier: 'Tolgee' }
      )}
    >
      <Text style={{ margin: '0 0 16px' }}>
        <If condition="${recipientName}">
          <If.Then>
            <LocalizedText
              keyName="email-greetings"
              defaultValue="Hello {recipientName}! 👋,"
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
      <Section>
        <Text style={{ margin: '0' }}>
          <Var
            variable="content"
            demoValue="<p>This is where your custom HTML content will appear.</p>"
            dangerouslyInjectValueAsHtmlWithoutSanitization={true}
          />
        </Text>
      </Section>
      <Text>
        <LocalizedText
          keyName="email-signature"
          defaultValue={'Happy translating,\nTolgee'}
        />
      </Text>
    </ClassicLayout>
  );
}
