/*
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
import { Hr, Link, Text } from '@react-email/components';
import ClassicLayout from '../../components/layouts/ClassicLayout';
import TolgeeLink from '../../components/atoms/TolgeeLink';
import _LocalizedText from '../../components/LocalizedText';
import Var from '../../components/Var';
import If from '../../components/If';
import For from '../../components/For';

export default function TestEmail() {
  return (
    <ClassicLayout
      subject="Test email (written with React Email)"
      sendReason="This email was sent for no reason. Hehe!"
    >
      <Text style={{ margin: '0 0 16px' }}>
        This is a test email exclusively used internally to test email
        rendering. If you received this by mistake, well... Whoops! This means
        we made an oopsie and you're free to make fun of us on social media...
      </Text>
      <Hr />
      <Text>
        <_LocalizedText
          keyName="email-test-string"
          defaultValue="Testing ICU strings -- {testVar}"
          demoParams={{
            link: (c) => (
              <TolgeeLink href="https://www.youtube.com/watch?v=dQw4w9WgXcQ">
                {c}
              </TolgeeLink>
            ),
            testVar: 'demo',
          }}
        />
      </Text>
      <Text>
        Value of `testVar`: <Var demoValue="demo" variable="testVar" />
      </Text>
      <Text>
        <span>Was `testVar` equal to "meow" : </span>
        <If condition="${testVar == 'meow'}">
          <If.Then>
            <span>yes</span>
          </If.Then>
          <If.Else>
            <span>no</span>
          </If.Else>
        </If>
      </Text>
      <ul className="text-sm">
        <For each="item, iterStat : ${testList}" demoIterations={3}>
          <li>
            <Var variable="iterStat.index" demoValue="1" />
            <ul>
              <li>
                Plain test: <Var variable="item.name" demoValue="demo..." />
              </li>
              <li>
                <_LocalizedText
                  keyName="email-test-it"
                  defaultValue="ICU test: {item__name}"
                  demoParams={{ item__name: 'demo...' }}
                />
              </li>
            </ul>
          </li>
        </For>
      </ul>

      <_LocalizedText
        keyName="powered-by"
        defaultValue="Powered by <link>Tolgee</link> 🐁"
        demoParams={{
          link: (c) => (
            <Link href="https://tolgee.io" className="text-inherit underline">
              {c}
            </Link>
          ),
        }}
      />
    </ClassicLayout>
  );
}
