/* eslint-disable no-restricted-imports */
import * as ee from './eeModule.ee';
import * as oss from './eeModule.oss';
import { EeModuleType } from './EeModuleType';

// check if ee modules share the same API
ee satisfies EeModuleType;
oss satisfies EeModuleType;
