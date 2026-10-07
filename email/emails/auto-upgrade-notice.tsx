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
import TolgeeButton from '../components/atoms/TolgeeButton';
import LocalizedText from '../components/LocalizedText';
import If from '../components/If';

export default function AutoUpgradeNoticeEmail() {
  return (
    <ClassicLayout
      subject={t.raw(
        'auto-upgrade-notice-subject',
        'Your Tolgee subscription will be auto-upgraded'
      )}
      sendReason={t.raw(
        'auto-upgrade-notice-send-reason',
        "You're receiving this email because you're an owner of the organization {organizationName} on {instanceQualifier}",
        { organizationName: 'My Organization', instanceQualifier: 'Tolgee' }
      )}
    >
      <Text style={{ margin: '0 0 16px' }}>
        <If condition="${recipientName}">
          <If.Then>
            <LocalizedText
              keyName="auto-upgrade-notice-greetings"
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
          keyName="auto-upgrade-notice-usage"
          defaultValue="Your organization is using <b>{currentWords}</b> of the <b>{includedWords}</b> words included in its current Tolgee plan."
          demoParams={{
            currentWords: '161,000',
            includedWords: '150,000',
          }}
        />
      </Text>
      <Text>
        <LocalizedText
          keyName="auto-upgrade-notice-why"
          defaultValue="Auto-upgrade is switched on for <b>{organizationName}</b>, so nothing was blocked and nothing was charged mid-month."
          demoParams={{ organizationName: 'Overflow Media' }}
        />{' '}
        <If condition="${renewalDate}">
          <If.Then>
            <LocalizedText
              keyName="auto-upgrade-notice-renewal-dated"
              defaultValue="At your renewal on <b>{renewalDate}</b> it moves to <b>{targetWords}</b> words."
              demoParams={{
                renewalDate: '9 November 2026',
                targetWords: '200,000',
              }}
            />
          </If.Then>
          <If.Else>
            <LocalizedText
              keyName="auto-upgrade-notice-renewal"
              defaultValue="At your next renewal it moves to <b>{targetWords}</b> words."
              demoParams={{ targetWords: '200,000' }}
            />
          </If.Else>
        </If>{' '}
        <If condition="${newPrice}">
          <If.Then>
            <If condition="${currentPrice}">
              <If.Then>
                <LocalizedText
                  keyName="auto-upgrade-notice-price-from"
                  defaultValue="Based on your usage today that would go from <b>{currentPrice}</b> to <b>{newPrice}</b> per {period, select, YEARLY {year} other {month}}."
                  demoParams={{
                    newPrice: '€495',
                    currentPrice: '€395',
                    period: 'MONTHLY',
                  }}
                />
              </If.Then>
              <If.Else>
                <LocalizedText
                  keyName="auto-upgrade-notice-price"
                  defaultValue="Based on your usage today that would be <b>{newPrice}</b> per {period, select, YEARLY {year} other {month}}."
                  demoParams={{ newPrice: '€495', period: 'MONTHLY' }}
                />
              </If.Else>
            </If>
          </If.Then>
          <If.Else />
        </If>
      </Text>
      <Text>
        <LocalizedText
          keyName="auto-upgrade-notice-may-change"
          defaultValue="If your usage changes before your renewal, the tier and the price change with it."
        />
      </Text>
      <Section className="text-center my-[24px]">
        <TolgeeButton data-th-href="${subscriptionsUrl}">
          <LocalizedText
            keyName="auto-upgrade-notice-cta"
            defaultValue="See your current estimate"
          />
        </TolgeeButton>
      </Section>
      <Text>
        <LocalizedText
          keyName="email-signature"
          defaultValue={'Kind Regards,\nTolgee'}
        />
      </Text>
    </ClassicLayout>
  );
}
