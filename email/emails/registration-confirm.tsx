/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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
import Layout from '../components/Layout';
import LocalizedText from '../components/LocalizedText';
import TolgeeLink from '../components/parts/TolgeeLink';
import TolgeeButton from '../components/parts/TolgeeButton';

export default function RegistrationConfirmEmail() {
  return (
    <Layout
      subject={t('registration-confirm-subject', 'Confirm your account')}
      sendReason={t(
        'send-reason-created-account',
        "You're receiving this email because you've created an account on {instanceQualifier}",
        { instanceQualifier: 'Tolgee' }
      )}
    >
      <Text>
        <LocalizedText
          keyName="email-greetings"
          defaultValue="Hello {username},"
          demoParams={{ username: 'Bob' }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="email-greetings"
          defaultValue="Welcome and thank you for creating an account! To start using Tolgee, you need to confirm your email."
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
          defaultValue="or copy and paste this URL into your browser: <link>{confirmUrl}</link>"
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
          keyName="registration-confirm-enjoy-your-stay"
          defaultValue="We hope you'll enjoy your experience!"
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="email-signature"
          defaultValue={'Regards,\nTolgee'}
        />
      </Text>
    </Layout>
  );
}
