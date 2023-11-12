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
import { renderToString } from 'react-dom/server';
import { convert } from 'html-to-text';

import {
  Html,
  Head,
  Tailwind,
  Body,
  Container,
  Section,
  Column,
  Row,
  Heading,
  Hr,
  Link,
  Text,
} from '@react-email/components';
import If from './If';
import ImgResource from './ImgResource';
import LocalizedText from './LocalizedText';

type Props = {
  children: React.ReactNode;
  subject: React.ReactElement;
  sendReason: React.ReactElement;
};

export default function Layout({ children, subject, sendReason }: Props) {
  const subjectPlainText = convert(renderToString(subject));

  return (
    <Html>
      <Head>
        <title {...{ 'th:text': subject.props['th:text'] }}>
          {subjectPlainText}
        </title>
        {process.env.NODE_ENV !== 'production' && (
          // This is a hack to get line returns to behave as line returns.
          // The Kotlin renderer will handle these cases, but this is for the browser preview.
          // white-space is poorly supported in email clients anyway.
          <style
            dangerouslySetInnerHTML={{ __html: 'p { white-space: pre-line; }' }}
          />
        )}
      </Head>
      <Tailwind
        config={{
          theme: {
            extend: {
              colors: {
                brand: '#ec407a',
              },
            },
          },
        }}
      >
        <Body className="bg-white my-auto mx-auto font-sans">
          <Container className="border border-solid border-[#eaeaea] rounded my-[40px] mx-auto w-[610px]">
            <Section className="p-[20px]">
              <Row>
                <Column>
                  <ImgResource
                    resourceName="tolgee_logo_text.png"
                    alt="Tolgee logo"
                  />
                </Column>
                <Column className="text-right">
                  <Heading className="text-xl text-brand m-0">
                    {subject}
                  </Heading>
                </Column>
              </Row>
            </Section>
            <Section className="border-y border-solid border-[#eaeaea] p-[20px]">
              {children}
            </Section>
            <Hr className="hidden" />
            <Section className="p-[10px] text-xs text-gray-600 text-center">
              <Text className="text-xs m-0 mb-2">{sendReason}</Text>
              <Container>
                <Column>
                  <Row>
                    <If condition="${isCloud}">
                      <Container>
                        <Row>
                          <Column>
                            <Text className="text-xs m-0">
                              <LocalizedText
                                keyName="footer-cloud-sent-by"
                                defaultValue="🐭 Sent by Tolgee - Check out <link>blog</link> and our socials! 🧀"
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
                            <Text className="text-xs m-0">
                              <Link href="https://twitter.com/tolgee_i18n">
                                <span className="hidden">Twitter</span>
                                <ImgResource
                                  className="mx-auto"
                                  resourceName="twitter.png"
                                  alt="Twitter"
                                  aria-hidden={true}
                                />
                              </Link>
                            </Text>
                          </Column>
                          <Column>
                            <Text className="text-xs m-0">
                              <Link href="https://www.facebook.com/Tolgee.i18n">
                                <span className="hidden">Facebook</span>
                                <ImgResource
                                  className="mx-auto"
                                  resourceName="facebook.png"
                                  alt="Facebook"
                                  aria-hidden={true}
                                />
                              </Link>
                            </Text>
                          </Column>
                          <Column>
                            <Text className="text-xs m-0">
                              <Link href="https://github.com/tolgee">
                                <span className="hidden">GitHub</span>
                                <ImgResource
                                  className="mx-auto"
                                  resourceName="github.png"
                                  alt="GitHub"
                                  aria-hidden={true}
                                />
                              </Link>
                            </Text>
                          </Column>
                          <Column>
                            <Text className="text-xs m-0">
                              <Link href="https://tolg.ee/slack">
                                <span className="hidden">Slack community</span>
                                <ImgResource
                                  className="mx-auto"
                                  resourceName="slack.png"
                                  alt="Slack community"
                                  aria-hidden={true}
                                />
                              </Link>
                            </Text>
                          </Column>
                          <Column>
                            <Text className="text-xs m-0">
                              <Link href="https://www.linkedin.com/company/tolgee">
                                <span className="hidden">LinkedIn</span>
                                <ImgResource
                                  className="mx-auto"
                                  resourceName="linkedin.png"
                                  alt="LinkedIn"
                                  aria-hidden={true}
                                />
                              </Link>
                            </Text>
                          </Column>
                        </Row>
                        <Row>
                          <Column>
                            <Text className="text-xs m-0">
                              <LocalizedText
                                keyName="footer-cloud-address"
                                defaultValue="Letovická 1421/22, Řečkovice, 621 00 Brno, Czech Republic"
                              />
                            </Text>
                          </Column>
                        </Row>
                      </Container>
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
                    </If>
                  </Row>
                </Column>
              </Container>
            </Section>
          </Container>
        </Body>
      </Tailwind>
    </Html>
  );
}
