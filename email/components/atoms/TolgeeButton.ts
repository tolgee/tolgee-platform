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
import { Button, ButtonProps } from '@react-email/components';

const BUTTON_CLASSES =
  'bg-brand text-white font-bold px-[16px] py-[8px] rounded cursor-pointer';

export default function TolgeeButton(props: ButtonProps) {
  const className = props.className
    ? `${BUTTON_CLASSES} ${props.className}`
    : BUTTON_CLASSES;

  return React.createElement(Button, { ...props, className: className });
}
