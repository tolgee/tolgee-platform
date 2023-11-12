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
import { join, extname } from 'path';
import { readFileSync, readdirSync } from 'fs';
import { Img, ImgProps } from '@react-email/components';

let root = __dirname;
while (!readdirSync(root).includes('resources') && root !== '/') {
  root = join(root, '..');
}

const RESOURCES_FOLDER = join(root, 'resources');

type Props = Omit<ImgProps, 'src'> & {
  resourceName: string;
};

export default function ImgResource(props: Props) {
  const file = join(RESOURCES_FOLDER, props.resourceName);

  const newProps = { ...props } as ImgProps & Props;
  delete newProps.resourceName;
  delete newProps.src;

  if (process.env.NODE_ENV === 'production') {
    // Resources will be copied during final assembly.
    newProps[
      'data-th-src'
    ] = `\${instanceUrl} + '/static/emails/${props.resourceName}'`;
  } else {
    const blob = readFileSync(file);
    const ext = extname(file).slice(1);
    newProps.src = `data:image/${ext};base64,${blob.toString('base64')}`;
  }

  return React.createElement(Img, newProps);
}
