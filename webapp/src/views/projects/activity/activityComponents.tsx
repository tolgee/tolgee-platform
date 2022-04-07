import { SetTranslationsActivity } from './activities/translation/SetTranslationsActivity';
import { KeyDeleteActivity } from './activities/key/KeyDeleteActivity';
import { TranslationCommentAddActivity } from './activities/translation/comment/TranslationCommentAddActivity';
import { TranslationCommentDeleteActivity } from './activities/translation/comment/TranslationCommentDeleteActivity';
import { TranslationCommentSetStateActivity } from './activities/translation/comment/TranslationCommentSetStateActivity';
import { KeyCreateActivity } from './activities/key/KeyCreateActivity';
import { KeyTagsEditActivity } from './activities/key/KeyTagsEditActivity';
import { KeyNameEditActivity } from './activities/key/KeyNameEditActivity';
import { ImportActivity } from './activities/ImportActivity';
import { components } from 'tg.service/apiSchema.generated';
import { ScreenshotAddActivity } from './activities/key/screenshot/ScreenshotAddActivity';
import { ScreenshotDeleteActivity } from './activities/key/screenshot/ScreenshotDeleteActivity';
import { SetTranslationsStateActivity } from './activities/translation/SetTranslationsStateActivity';

export const activityComponents = {
  SET_TRANSLATIONS: SetTranslationsActivity,
  SET_TRANSLATION_STATE: SetTranslationsStateActivity,
  KEY_DELETE: KeyDeleteActivity,
  TRANSLATION_COMMENT_ADD: TranslationCommentAddActivity,
  TRANSLATION_COMMENT_DELETE: TranslationCommentDeleteActivity,
  TRANSLATION_COMMENT_SET_STATE: TranslationCommentSetStateActivity,
  CREATE_KEY: KeyCreateActivity,
  KEY_TAGS_EDIT: KeyTagsEditActivity,
  KEY_NAME_EDIT: KeyNameEditActivity,
  IMPORT: ImportActivity,
  SCREENSHOT_ADD: ScreenshotAddActivity,
  SCREENSHOT_DELETE: ScreenshotDeleteActivity,
} as Record<components['schemas']['ProjectActivityModel']['type'], any>;
