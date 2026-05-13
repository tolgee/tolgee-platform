import { useTranslate } from '@tolgee/react';

export function useErrorTranslation() {
  const { t } = useTranslate();

  return (code: string, params?: string[]) => {
    switch (code.toLocaleLowerCase()) {
      // from 'ApiHttpService.tsx'
      case 'authentication_cancelled':
        return t('authentication_cancelled');

      case 'bad_credentials':
        return t('bad_credentials');
      case 'invalid_otp_code':
        return t('invalid_otp_code');
      case 'invitation_code_does_not_exist_or_expired':
        return t('invitation_code_does_not_exist_or_expired');
      case 'invitation_email_mismatch':
        return t('invitation_email_mismatch');
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
      case 'third_party_auth_no_email':
        return t('third_party_auth_no_email');
      case 'third_party_auth_non_matching_email':
        return t('third_party_auth_non_matching_email');
      case 'third_party_switch_initiated':
        return t('third_party_switch_initiated');
      case 'third_party_switch_conflict':
        return t('third_party_switch_conflict');
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
      case 'out_of_credits':
        return t('out_of_credits');
      case 'key_exists_in_namespace':
        return t('key_exists_in_namespace');
      case 'translation_api_rate_limit':
        return t('translation_api_rate_limit');
      case 'translation_failed':
        return t('translation_failed');
      case 'language_not_supported_by_service':
        return t('language_not_supported_by_service');
      case 'formality_not_supported_by_service':
        return t('formality_not_supported_by_service');
      case 'cannot_modify_disabled_translation':
        return t('cannot_modify_disabled_translation');
      case 'invalid_connection_string':
        return t('invalid_connection_string');
      case 'content_storage_config_invalid':
        return t('content_storage_config_invalid');
      case 'content_storage_test_failed':
        return t('content_storage_test_failed');
      case 'content_storage_is_in_use':
        return t('content_storage_is_in_use');
      case 'plan_has_subscribers':
        return t('plan_has_subscribers');
      case 'cannot_store_file_to_content_storage':
        return t('cannot_store_file_to_content_storage');
      case 'unexpected_error_while_publishing_to_content_storage':
        return t('unexpected_error_while_publishing_to_content_storage');
      case 'webhook_responded_with_non_200_status':
        return t('webhook_responded_with_non_200_status');
      case 'unexpected_error_while_executing_webhook':
        return t('unexpected_error_while_executing_webhook');
      case 'resource_not_found':
        return t('resource_not_found');
      case 'subscription_already_canceled':
        return t('subscription_already_canceled');
      case 'credit_spending_limit_exceeded':
        return t('credit_spending_limit_exceeded');
      case 'subscription_not_active':
        return t('subscription_not_active');
      case 'invalid_plural_form':
        return t('invalid_plural_form');
      case 'slack_not_configured':
        return t('slack_not_configured');
      case 'slack_workspace_already_connected':
        return t('slack_workspace_already_connected');
      case 'email_already_verified':
        return t('verify_email_already_verified');
      case 'email_verification_code_not_valid':
        return t('verify_email_verification_code_not_valid');
      case 'user_is_subscribed_to_paid_plan':
        return t('user_is_subscribed_to_paid_plan');
      case 'sso_token_exchange_failed':
        return t('sso_token_exchange_failed');
      case 'sso_user_info_retrieval_failed':
        return t('sso_user_info_retrieval_failed');
      case 'sso_id_token_expired':
        return t('sso_id_token_expired');
      case 'sso_user_cannot_create_organization':
        return t('sso_user_cannot_create_organization');
      case 'sso_cant_verify_user':
        return t('sso_cant_verify_user');
      case 'sso_auth_missing_domain':
        return t('sso_auth_missing_domain');
      case 'sso_domain_not_found_or_disabled':
        return t('sso_domain_not_found_or_disabled');
      case 'native_authentication_disabled':
        return t('native_authentication_disabled');
      case 'operation_unavailable_for_account_type':
        return t('operation_unavailable_for_account_type');
      case 'invitation_organization_mismatch':
        return t('invitation_organization_mismatch');
      case 'user_is_managed_by_organization':
        return t('user_is_managed_by_organization');
      case 'cannot_set_sso_provider_missing_fields':
        return t('cannot_set_sso_provider_missing_fields');
      case 'namespace_cannot_be_used_when_feature_is_disabled':
        return t('namespace_cannot_be_used_when_feature_is_disabled');
      case 'namespaces_cannot_be_disabled_when_namespace_exists':
        return t('namespaces_cannot_be_disabled_when_namespace_exists');
      case 'sso_domain_not_allowed':
        return t('sso_domain_not_allowed');
      case 'sso_login_forced_for_this_account':
        return t('sso_login_forced_for_this_account');
      case 'use_sso_for_authentication_instead':
        return t('use_sso_for_authentication_instead');
      case 'user_missing_password':
        return t('user_missing_password');
      case 'invalid_jwt_token':
        return t('expired_jwt_token');
      case 'free_self_hosted_seat_limit_exceeded':
        return t('free_self_hosted_seat_limit_exceeded');
      case 'plan_seat_limit_exceeded':
        return t('plan_seat_limit_exceeded');
      case 'llm_provider_not_found':
        return t('llm_provider_not_found', { value: params?.[0] || '' });
      case 'llm_provider_error':
        return t('llm_provider_error', { value: params?.[0] || '' });
      case 'llm_template_parsing_error':
        return t('llm_template_parsing_error', {
          value: params?.[0] || '',
          line: params?.[1] || '',
          column: params?.[2] || '',
        });
      case 'llm_rate_limited':
        return t('llm_rate_limited');
      case 'llm_provider_not_returned_json':
        return t('llm_provider_not_returned_json');
      case 'prompt_not_found':
        return t('prompt_not_found');
      case 'llm_content_filter':
        return t('llm_content_filter');
      case 'llm_provider_empty_response':
        return t('llm_provider_empty_response');
      case 'label_already_exists':
        return t('label_already_exists', { value: params?.[0] || '' });
      case 'cannot_modify_reviewed_translation':
        return t('cannot_modify_reviewed_translation');
      case 'suggestion_cant_be_plural':
        return t('suggestion_cant_be_plural');
      case 'suggestion_must_be_plural':
        return t('suggestion_must_be_plural');
      case 'duplicate_suggestion':
        return t('duplicate_suggestion');
      case 'operation_not_permitted_in_read_only_mode':
        return t('operation_not_permitted_in_read_only_mode');
      case 'cannot_delete_branch_with_children':
        return t('cannot_delete_branch_with_children');
      case 'feature_not_enabled_for_project':
        return t('feature_not_enabled_for_project', {
          feature: params?.[0] || '',
        });
      case 'export_key_plural_suffix_collision':
        return t('export_key_plural_suffix_collision', {
          pluralKey: params?.[0] || '',
          collidingKey: params?.[1] || '',
          suffix: params?.[2] || '',
        });

      // Authentication
      case 'unauthenticated':
        return t('unauthenticated');
      case 'api_access_forbidden':
        return t('api_access_forbidden');
      case 'mfa_enabled':
        return t('mfa_enabled');
      case 'mfa_not_enabled':
        return t('mfa_not_enabled');
      case 'username_or_password_invalid':
        return t('username_or_password_invalid');
      case 'can_not_revoke_own_permissions':
        return t('can_not_revoke_own_permissions');
      case 'third_party_auth_no_sub':
        return t('third_party_auth_no_sub');
      case 'third_party_auth_unknown_error':
        return t('third_party_auth_unknown_error');
      case 'third_party_unauthorized':
        return t('third_party_unauthorized');
      case 'third_party_google_workspace_mismatch':
        return t('third_party_google_workspace_mismatch');
      case 'invalid_authentication_method':
        return t('invalid_authentication_method');
      case 'authentication_method_disabled':
        return t('authentication_method_disabled');

      // API keys & personal access tokens
      case 'api_key_not_found':
        return t('api_key_not_found');
      case 'invalid_api_key':
        return t('invalid_api_key');
      case 'invalid_project_api_key':
        return t('invalid_project_api_key');
      case 'project_api_key_expired':
        return t('project_api_key_expired');
      case 'pat_not_found':
        return t('pat_not_found');
      case 'invalid_pat':
        return t('invalid_pat');
      case 'pat_expired':
        return t('pat_expired');
      case 'pat_access_not_allowed':
        return t('pat_access_not_allowed');
      case 'pak_access_not_allowed':
        return t('pak_access_not_allowed');

      // User & account
      case 'user_not_found':
        return t('user_not_found');
      case 'cannot_delete_your_own_account':
        return t('cannot_delete_your_own_account');
      case 'cannot_disable_your_own_account':
        return t('cannot_disable_your_own_account');
      case 'current_password_required':
        return t('current_password_required');

      // Organizations
      case 'organization_not_found':
        return t('organization_not_found');
      case 'cannot_create_organization':
        return t('cannot_create_organization');
      case 'slug_not_unique':
        return t('slug_not_unique');
      case 'user_is_not_member_of_organization':
        return t('user_is_not_member_of_organization');
      case 'user_is_organization_owner':
        return t('user_is_organization_owner');
      case 'user_is_organization_member':
        return t('user_is_organization_member');
      case 'cannot_set_your_own_permissions':
        return t('cannot_set_your_own_permissions');
      case 'cannot_set_your_own_role':
        return t('cannot_set_your_own_role');
      case 'user_cannot_view_this_organization':
        return t('user_cannot_view_this_organization');
      case 'user_is_not_owner_of_organization':
        return t('user_is_not_owner_of_organization');
      case 'user_is_not_owner_or_maintainer_of_organization':
        return t('user_is_not_owner_or_maintainer_of_organization');
      case 'dont_have_direct_permissions':
        return t('dont_have_direct_permissions');
      case 'organization_not_subscribed':
        return t('organization_not_subscribed');

      // Projects
      case 'project_not_found':
        return t('project_not_found');
      case 'cannot_leave_owning_project':
        return t('cannot_leave_owning_project');
      case 'cannot_leave_project_with_organization_role':
        return t('cannot_leave_project_with_organization_role');

      // Languages
      case 'language_not_found':
        return t('language_not_found');
      case 'cannot_delete_base_language':
        return t('cannot_delete_base_language');
      case 'cannot_add_more_then_100_languages':
        return t('cannot_add_more_then_100_languages');
      case 'cannot_add_more_then_1000_languages':
        return t('cannot_add_more_then_1000_languages');
      case 'language_not_permitted':
        return t('language_not_permitted');

      // Keys & translations
      case 'key_not_found':
        return t('key_not_found');
      case 'translation_not_found':
        return t('translation_not_found');
      case 'translation_text_too_long':
        return t('translation_text_too_long');
      case 'translation_exists':
        return t('translation_exists');
      case 'cannot_set_state_for_missing_translation':
        return t('cannot_set_state_for_missing_translation');
      case 'cannot_translate_base_language':
        return t('cannot_translate_base_language');
      case 'cannot_modify_keys':
        return t('cannot_modify_keys');

      // Namespaces
      case 'namespace_not_found':
        return t('namespace_not_found');
      case 'namespace_exists':
        return t('namespace_exists');

      // Screenshots & file uploads
      case 'file_not_image':
        return t('file_not_image');
      case 'max_screenshots_exceeded':
        return t('max_screenshots_exceeded');
      case 'too_many_uploaded_images':
        return t('too_many_uploaded_images');
      case 'screenshot_not_of_key':
        return t('screenshot_not_of_key');
      case 'unsupported_media_type':
        return t('unsupported_media_type');
      case 'file_processing_failed':
        return t('file_processing_failed');

      // Comments
      case 'can_edit_only_own_comment':
        return t('can_edit_only_own_comment');

      // Tags
      case 'tag_not_found':
        return t('tag_not_found');
      case 'tag_too_log':
        return t('tag_too_log');
      case 'tag_is_blank':
        return t('tag_is_blank');

      // Import
      case 'cannot_parse_file':
        return t('cannot_parse_file');
      case 'import_has_expired':
        return t('import_has_expired');
      case 'import_keys_error':
        return t('import_keys_error');
      case 'no_data_to_import':
        return t('no_data_to_import');
      case 'import_failed':
        return t('import_failed');

      // Machine translation
      case 'no_auto_translation_method':
        return t('no_auto_translation_method');
      case 'advanced_params_not_supported':
        return t('advanced_params_not_supported');

      // Rate limiting
      case 'too_many_requests':
        return t('too_many_requests');
      case 'rate_limited':
        return t('rate_limited');

      // Plural forms
      case 'plural_forms_not_found_for_language':
        return t('plural_forms_not_found_for_language');
      case 'nested_plurals_not_supported':
        return t('nested_plurals_not_supported');
      case 'message_is_not_plural':
        return t('message_is_not_plural');
      case 'content_outside_plural_forms':
        return t('content_outside_plural_forms');
      case 'multiple_plurals_not_supported':
        return t('multiple_plurals_not_supported');
      case 'plural_forms_data_loss':
        return t('plural_forms_data_loss');

      // Content storage
      case 'azure_config_required':
        return t('azure_config_required');
      case 's3_config_required':
        return t('s3_config_required');
      case 'content_storage_config_required':
        return t('content_storage_config_required');

      // Billing & subscriptions
      case 'plan_not_found':
        return t('plan_not_found');
      case 'plan_not_available_any_more':
        return t('plan_not_available_any_more');
      case 'subscription_not_found':
        return t('subscription_not_found');
      case 'license_key_used_by_another_instance':
        return t('license_key_used_by_another_instance');
      case 'this_instance_is_already_licensed':
        return t('this_instance_is_already_licensed');
      case 'cannot_subscribe_to_free_plan':
        return t('cannot_subscribe_to_free_plan');
      case 'subscription_not_scheduled_for_cancellation':
        return t('subscription_not_scheduled_for_cancellation');
      case 'cannot_cancel_trial':
        return t('cannot_cancel_trial');
      case 'cannot_update_without_modification':
        return t('cannot_update_without_modification');
      case 'current_subscription_is_not_trialing':
        return t('current_subscription_is_not_trialing');
      case 'date_has_to_be_in_the_future':
        return t('date_has_to_be_in_the_future');
      case 'plan_key_limit_exceeded':
        return t('plan_key_limit_exceeded');
      case 'keys_spending_limit_exceeded':
        return t('keys_spending_limit_exceeded');

      // Batch jobs
      case 'batch_job_cancellation_timeout':
        return t('batch_job_cancellation_timeout');
      case 'multiple_items_in_chunk_failed':
        return t('multiple_items_in_chunk_failed');

      // Slack integration
      case 'slack_workspace_not_found':
        return t('slack_workspace_not_found');
      case 'slack_missing_scope':
        return t('slack_missing_scope');
      case 'slack_not_connected_to_your_account':
        return t('slack_not_connected_to_your_account');
      case 'slack_not_subscribed_yet':
        return t('slack_not_subscribed_yet');
      case 'slack_connection_failed':
        return t('slack_connection_failed');
      case 'tolgee_account_already_connected':
        return t('tolgee_account_already_connected');
      case 'slack_connection_error':
        return t('slack_connection_error');

      // Tasks
      case 'task_not_found':
        return t('task_not_found');
      case 'task_not_finished':
        return t('task_not_finished');
      case 'task_not_open':
        return t('task_not_open');
      case 'translation_agency_not_found':
        return t('translation_agency_not_found');

      // LLM / AI
      case 'llm_provider_timeout':
        return t('llm_provider_timeout');
      case 'no_llm_provider_configured':
        return t('no_llm_provider_configured');

      // Glossary
      case 'glossary_not_found':
        return t('glossary_not_found');
      case 'glossary_term_not_found':
        return t('glossary_term_not_found');
      case 'glossary_term_translation_not_found':
        return t('glossary_term_translation_not_found');
      case 'glossary_non_translatable_term_cannot_be_translated':
        return t('glossary_non_translatable_term_cannot_be_translated');

      // Labels
      case 'label_not_found':
        return t('label_not_found');
      case 'filter_by_value_label_not_valid':
        return t('filter_by_value_label_not_valid');

      // Suggestions
      case 'suggestion_not_found':
        return t('suggestion_not_found');
      case 'user_can_only_delete_his_suggestions':
        return t('user_can_only_delete_his_suggestions');

      // Impersonation
      case 'impersonation_of_admin_by_supporter_not_allowed':
        return t('impersonation_of_admin_by_supporter_not_allowed');
      case 'already_impersonating_user':
        return t('already_impersonating_user');

      // Branching
      case 'branch_not_found':
        return t('branch_not_found');
      case 'cannot_delete_default_branch':
        return t('cannot_delete_default_branch');
      case 'branch_already_exists':
        return t('branch_already_exists');
      case 'origin_branch_not_found':
        return t('origin_branch_not_found');
      case 'branch_merge_not_found':
        return t('branch_merge_not_found');
      case 'branch_merge_revision_not_valid':
        return t('branch_merge_revision_not_valid');
      case 'branch_merge_conflicts_not_resolved':
        return t('branch_merge_conflicts_not_resolved');
      case 'branch_merge_already_merged':
        return t('branch_merge_already_merged');
      case 'branching_not_enabled_for_project':
        return t('branching_not_enabled_for_project');

      // OSS limitations
      case 'this_feature_is_not_implemented_in_oss':
        return t('this_feature_is_not_implemented_in_oss');

      case 'qa_checks_not_enabled':
        return t('qa_checks_not_enabled');
      default:
        return code;
    }
  };
}
