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
import { renderToString } from 'react-dom/server';
import { convert } from 'html-to-text';

import { Head, Html, Tailwind } from '@react-email/components';

type Props = {
  children: React.ReactNode;
  subject: React.ReactElement;
};

export default function LayoutCore({ children, subject }: Props) {
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
        {children}
      </Tailwind>
    </Html>
  );
}
