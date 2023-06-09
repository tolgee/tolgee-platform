import { useTranslate } from '@tolgee/react';

export function useErrorTranslation() {
  const { t } = useTranslate();

  return (code: string) => {
    switch (code) {
      case 'bad_credentials':
        return t('bad_credentials');
      case 'invalid_otp_code':
        return t('invalid_otp_code');
      case 'invitation_code_does_not_exist_or_expired':
        return t('invitation_code_does_not_exist_or_expired');
      case 'language_tag_exists':
        return t('language_tag_exists');
      case 'language_name_exists':
        return t('language_name_exists');
      case 'operation_not_permitted':
        return t('operation_not_permitted');
      case 'registrations_not_allowed':
        return t('registrations_not_allowed');
      case 'key_exists':
        return t('key_exists');
      case 'third_party_auth_error_message':
        return t('third_party_auth_error_message');
      case 'username_already_exists':
        return t('username_already_exists');
      case 'user_already_has_permissions':
        return t('user_already_has_permissions');
      case 'user_already_has_role':
        return t('user_already_has_role');
      case 'file_too_big':
        return t('file_too_big');
      case 'email_not_verified':
        return t('email_not_verified');
      case 'missing_callback_url':
        return t('missing_callback_url');
      case 'expired_jwt_token':
        return t('expired_jwt_token');
      case 'organization_has_no_other_owner':
        return t('organization_has_no_other_owner');
      case 'existing_language_not_selected':
        return t('existing_language_not_selected');
      case 'conflict_is_not_resolved':
        return t('conflict_is_not_resolved');
      case 'request_parse_error':
        return t('request_parse_error');
      case 'invalid_recaptcha_token':
        return t('invalid_recaptcha_token');
      case 'no_exported_result':
        return t('no_exported_result');
      case 'email_already_invited_or_member':
        return t('email_already_invited_or_member');
      case 'validation_email_is_not_valid':
        return t('validation_email_is_not_valid');
      case 'wrong_current_password':
        return t('wrong_current_password');
      case 'unexpected_error_occurred':
        return t('unexpected_error_occurred');
      case 'feature_not_enabled':
        return t('feature_not_enabled');
      case 'license_key_not_found':
        return t('license_key_not_found');
      case 'plan_translation_limit_exceeded':
        return t('plan_translation_limit_exceeded');
      case 'organization_already_subscribed':
        return t('organization_already_subscribed');
      case 'user_has_no_project_access':
        return t('user_has_no_project_access');
      case 'translation_spending_limit_exceeded':
        return t('translation_spending_limit_exceeded');
      case 'seats_spending_limit_exceeded':
        return t('seats_spending_limit_exceeded');
      case 'mt_service_not_enabled':
        return t('mt_service_not_enabled');
      // from 'ApiHttpService.tsx'
      case 'authentication_cancelled':
        return t('authentication_cancelled');

      case 'plan_has_subscribers':
        return t('plan_has_subscribers');
      default:
        return code;
    }
  };
}
