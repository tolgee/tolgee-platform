import { Box } from '@mui/material';
import { ProfileAvatar } from 'tg.component/common/avatar/ProfileAvatar';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

type TranslationAgencyModel = components['schemas']['TranslationAgencyModel'];

type Props = {
  agency: TranslationAgencyModel;
};

export const TAProfileAvatar = ({ agency }: Props) => {
  const uploadLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/translation-agency/{agencyId}/avatar',
    method: 'put',
    invalidatePrefix: [
      '/v2/billing/translation-agency',
      '/v2/administration/billing/translation-agency',
    ],
  });

  const removeLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/translation-agency/{agencyId}/avatar',
    method: 'delete',
    invalidatePrefix: [
      '/v2/billing/translation-agency',
      '/v2/administration/billing/translation-agency',
    ],
  });

  return (
    <ProfileAvatar
      cropperProps={{
        style: {
          width: 300,
          height: 60,
        },
        viewMode: 0,
        aspectRatio: 5,
        rounded: false,
      }}
      owner={{
        type: 'PROJECT',
        avatar: agency.avatar,
        id: agency.id,
        name: agency.name,
      }}
      preview={(props) => {
        return props.avatar?.large ? (
          <img
            width={200}
            height={150}
            style={{ objectFit: 'contain', padding: '1px' }}
            src={props.avatar?.large}
          />
        ) : (
          <Box
            width={200}
            height={150}
            style={{ padding: '1px' }}
            display="grid"
            alignContent="center"
            justifyContent="center"
          >
            <div>No avatar</div>
          </Box>
        );
      }}
      onUpload={(blob: Blob) =>
        uploadLoadable.mutateAsync({
          path: {
            agencyId: agency.id,
          },
          content: {
            'multipart/form-data': {
              avatar: new File([blob], 'Avatar', { type: 'image/png' }) as any,
            },
          },
        })
      }
      onRemove={() =>
        removeLoadable.mutateAsync({
          path: {
            agencyId: agency.id,
          },
        })
      }
    />
  );
};
