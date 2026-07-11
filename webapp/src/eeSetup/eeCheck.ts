/* eslint-disable no-restricted-imports */
import * as ee from './eeModule.ee';
import * as oss from './eeModule.oss';

// check if ee modules share the same API
ee satisfies typeof oss;
oss satisfies typeof ee;
