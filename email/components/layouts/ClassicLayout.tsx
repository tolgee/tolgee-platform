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

import {
  Body,
  Column,
  Container,
  Heading,
  Hr,
  Link,
  Row,
  Section,
  Text,
} from '@react-email/components';
import If from '../If';
import ImgResource from '../ImgResource';
import LocalizedText from '../LocalizedText';
import LayoutCore from './LayoutCore';
import t, { TranslatedText } from '../translate';

type Props = {
  children: React.ReactNode;
  subject: TranslatedText | string;
  header?: React.ReactNode | string;
  sendReason: TranslatedText | string;
};

type SocialLinkProps = {
  social: string;
  link: string;
  resourceName: string;
};

function SocialLink({ social, link, resourceName }: SocialLinkProps) {
  return (
    <Text className="text-xs m-0">
      <Link href={link} aria-label={social}>
        <ImgResource className="mx-auto" resourceName={resourceName} alt="" />
      </Link>
    </Text>
  );
}

export default function ClassicLayout({
  children,
  subject,
  header,
  sendReason,
}: Props) {
  return (
    <LayoutCore subject={subject}>
      <Body className="bg-white my-auto mx-auto font-sans">
        <Container className="mx-auto my-[40px] max-w-[465px] rounded border border-[#eaeaea] border-solid">
          <Section className="p-[20px]">
            <Row>
              <Column>
                <ImgResource
                  resourceName="tolgee_logo_text.png"
                  alt="Tolgee logo"
                />
              </Column>
              {header && (
                <Column className="text-right">
                  <Heading className="text-xl text-brand m-0">
                    {typeof header === 'string' ? t.render(header) : header}
                  </Heading>
                </Column>
              )}
            </Row>
          </Section>
          <Section className="border-y border-solid border-[#eaeaea] p-[20px]">
            {children}
          </Section>
          <Hr className="hidden" />
          <Section className="p-[10px] text-xs text-gray-600 text-center">
            <Text className="text-xs m-0 mb-2">{t.render(sendReason)}</Text>
            <Container>
              <Row>
                <Column>
                  <If condition="${isCloud}">
                    <If.Then>
                      <Container>
                        <Row>
                          <Column>
                            <Text className="text-xs m-0">
                              <LocalizedText
                                keyName="footer-cloud-sent-by"
                                defaultValue="🐭 Sent by Tolgee - Check out our <link>blog</link> and our socials! 🧀"
                                demoParams={{
                                  link: (c) => (
                                    <Link
                                      className="text-inherit underline"
                                      href="https://tolgee.io/blog"
                                    >
                                      {c}
                                    </Link>
                                  ),
                                }}
                              />
                            </Text>
                          </Column>
                        </Row>
                        <Row className="w-3/4 mx-auto my-3">
                          <Column>
                            <SocialLink
                              social="Twitter (X)"
                              link="https://twitter.com/tolgee_i18n"
                              resourceName="twitter-x.png"
                            />
                          </Column>
                          <Column>
                            <SocialLink
                              social="Facebook"
                              link="https://www.facebook.com/Tolgee.i18n"
                              resourceName="facebook.png"
                            />
                          </Column>
                          <Column>
                            <SocialLink
                              social="GitHub"
                              link="https://github.com/tolgee"
                              resourceName="github.png"
                            />
                          </Column>
                          <Column>
                            <SocialLink
                              social="Slack community"
                              link="https://tolg.ee/slack"
                              resourceName="slack.png"
                            />
                          </Column>
                          <Column>
                            <SocialLink
                              social="LinkedIn"
                              link="https://www.linkedin.com/company/tolgee"
                              resourceName="linkedin.png"
                            />
                          </Column>
                        </Row>
                      </Container>
                    </If.Then>
                    <If.Else>
                      <Text className="text-xs m-0">
                        <LocalizedText
                          keyName="powered-by"
                          defaultValue="Powered by <link>Tolgee</link> 🐁"
                          demoParams={{
                            link: (c) => (
                              <Link
                                href="https://tolgee.io"
                                className="text-inherit underline"
                              >
                                {c}
                              </Link>
                            ),
                          }}
                        />
                      </Text>
                    </If.Else>
                  </If>
                </Column>
              </Row>
            </Container>
          </Section>
        </Container>
      </Body>
    </LayoutCore>
  );
}
