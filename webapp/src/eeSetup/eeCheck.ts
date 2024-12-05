/* eslint-disable no-restricted-imports */
import * as ee from './eePlugin.ee';
import * as oss from './eePlugin.oss';
import { EePluginType } from './EePluginType';

// check if ee modules share the same API
ee satisfies EePluginType;
oss satisfies EePluginType;
