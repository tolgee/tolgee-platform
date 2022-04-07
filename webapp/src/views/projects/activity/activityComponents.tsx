import { SetTranslationsActivity } from './activities/translation/SetTranslationsActivity';
import { KeyDeleteActivity } from './activities/key/KeyDeleteActivity';
import { TranslationCommentAddActivity } from './activities/translation/comment/TranslationCommentAddActivity';
import { TranslationCommentDeleteActivity } from './activities/translation/comment/TranslationCommentDeleteActivity';
import { TranslationCommentSetStateActivity } from './activities/translation/comment/TranslationCommentSetStateActivity';
import { KeyCreateActivity } from './activities/key/KeyCreateActivity';
import { KeyTagsEditActivity } from './activities/key/KeyTagsEditActivity';
import { KeyNameEditActivity } from './activities/key/KeyNameEditActivity';

export const activityComponents = {
  SET_TRANSLATIONS: SetTranslationsActivity,
  KEY_DELETE_ACTIVITY: KeyDeleteActivity,
  TRANSLATION_COMMENT_ADD: TranslationCommentAddActivity,
  TRANSLATION_COMMENT_DELETE: TranslationCommentDeleteActivity,
  TRANSLATION_COMMENT_SET_STATE: TranslationCommentSetStateActivity,
  CREATE_KEY: KeyCreateActivity,
  KEY_TAGS_EDIT: KeyTagsEditActivity,
  KEY_NAME_EDIT_ACTIVITY: KeyNameEditActivity,
};
