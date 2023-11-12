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
import { Link, LinkProps } from '@react-email/components';

const LINK_CLASSES = 'text-brand underline';

export default function TolgeeLink(props: LinkProps) {
  const className = props.className
    ? `${LINK_CLASSES} ${props.className}`
    : LINK_CLASSES;

  return React.createElement(Link, { ...props, className: className });
}
