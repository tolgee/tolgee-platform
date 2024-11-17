import * as React from 'react';
import { Hr, Text } from '@react-email/components';
import ClassicLayout from '../components/layouts/ClassicLayout';
import TolgeeLink from '../components/atoms/TolgeeLink';
import LocalizedText from '../components/LocalizedText';
import Var from '../components/Var';
import If from '../components/If';
import For from '../components/For';

export default function RegistrationConfirmEmail() {
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
        <LocalizedText
          keyName="__email-test-string"
          defaultValue="Testing ICU strings -- {testVar} <link>link text</link>"
          demoParams={{
            link: (c: string) => (
              <TolgeeLink href="https://www.youtube.com/watch?v=dQw4w9WgXcQ">
                {c}
              </TolgeeLink>
            ),
            testVar: 'demo',
          }}
        />
      </Text>
      <Text>
        Value of `testVar` : <Var demoValue="demo" variable="testVar" />
      </Text>
      <Text>
        <span>Was `testVar` equal to "meow" : </span>
        <If condition="${testVar == 'meow'}">
          <span>yes</span>
          <span>no</span>
        </If>
      </Text>
      <ul className="text-sm">
        <For each="item, iterStat : ${testList}" demoIterations={3}>
          <li>
            <Var variable="iterStat.index" demoValue="1" />
            <ul>
              <li>
                <Var variable="item.name" demoValue="demo..." />
              </li>
              <li>
                <LocalizedText
                  keyName="__email-test-it"
                  defaultValue="ICU test: {item__name}"
                  demoParams={{ item__name: 'demo...' }}
                />
              </li>
            </ul>
          </li>
        </For>
      </ul>
    </ClassicLayout>
  );
}
