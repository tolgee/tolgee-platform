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
import TolgeeLink from '../components/atoms/TolgeeLink';
import TolgeeButton from '../components/atoms/TolgeeButton';
import LocalizedText from '../components/LocalizedText';
import If from '../components/If';

export default function RegistrationConfirmEmail() {
  return (
    <ClassicLayout
      subject={t.raw('registration-confirm-subject', 'Confirm your account')}
      sendReason={t.raw(
        'send-reason-created-account',
        "You're receiving this email because you've created an account on {instanceQualifier}",
        { instanceQualifier: 'Tolgee' }
      )}
    >
      <Text style={{ margin: '0 0 16px' }}>
        <LocalizedText
          keyName="email-greetings"
          defaultValue="Hello {username},"
          demoParams={{ username: 'Bob' }}
        />
      </Text>
      <Text>
        <If condition="${isSignup}">
          <If.Then>
            <LocalizedText
              keyName="registration-welcome-text"
              defaultValue="Welcome and thank you for creating an account!"
            />{' '}
          </If.Then>
          <If.Else />
        </If>
        <LocalizedText
          keyName="registration-confirm-email-text"
          defaultValue="To start using Tolgee, you need to confirm your email."
        />
      </Text>
      <Section className="text-center my-[24px]">
        <TolgeeButton data-th-href="${confirmUrl}">
          <LocalizedText
            keyName="registration-confirm-cta"
            defaultValue="Confirm my email"
          />
        </TolgeeButton>
      </Section>
      <Text>
        <LocalizedText
          keyName="registration-confirm-link"
          defaultValue={
            'Or, copy and paste this URL into your browser:\n<link>{confirmUrl}</link>'
          }
          demoParams={{
            link: (c: string) => (
              <TolgeeLink href="https://www.youtube.com/watch?v=dQw4w9WgXcQ">
                {c}
              </TolgeeLink>
            ),
            confirmUrl: 'https://app.tolgee.io/login/verify_email?owowhatsthis',
          }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="registration-welcome-enjoy-your-stay"
          defaultValue="We hope you'll enjoy your experience!"
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="email-signature"
          defaultValue={'Happy translating,\nTolgee'}
        />
      </Text>
    </ClassicLayout>
  );
}
