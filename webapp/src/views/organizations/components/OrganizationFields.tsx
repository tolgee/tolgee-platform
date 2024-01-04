import React, { useEffect, useState } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { useDebounce } from 'use-debounce';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { LINKS, PARAMS } from 'tg.constants/links';
import { organizationService } from 'tg.service/OrganizationService';

type Props = {
  disabled?: boolean;
};

export const OrganizationFields: React.FC<Props> = ({ disabled }) => {
  const [slugDisabled, setSlugDisabled] = useState(true);

  const formik = useFormikContext();
  const [value] = useDebounce(formik.getFieldProps('name').value, 500);
  const slugValue = formik.getFieldProps('slug').value;

  useEffect(() => {
    const nameMeta = formik.getFieldMeta('name');
    const nameChanged = nameMeta.initialValue !== nameMeta.value;
    //const slugChanged = slugMeta.initialValue !== slugMeta.value

    if (nameChanged) {
      const initialSlug = formik.getFieldMeta('slug').initialValue;
      const slugNotTouchedOrEmpty =
        !formik.getFieldMeta('slug').touched || slugValue === '';
      //autogenerate the slug just when not touched and name is valid
      if (
        formik.getFieldMeta('name').error == undefined &&
        value != '' &&
        slugNotTouchedOrEmpty
      ) {
        organizationService
          .generateSlug(value, initialSlug as string)
          .then((slug) => {
            formik.getFieldHelpers('slug').setValue(slug);
            formik.getFieldHelpers('slug').setTouched(false);
          });
      }
    }
  }, [value]);

  return (
    <>
      <TextField
        variant="standard"
        data-cy={'organization-name-field'}
        fullWidth
        label={<T keyName="create_organization_name_label" />}
        name="name"
        required={true}
        disabled={disabled}
      />
      <Box
        onClick={() => setSlugDisabled(false)}
        style={{ cursor: slugDisabled ? 'pointer' : 'initial' }}
      >
        <TextField
          variant="standard"
          data-cy={'organization-address-part-field'}
          disabled={slugDisabled || disabled}
          fullWidth
          label={<T keyName="create_organization_slug_label" />}
          name="slug"
          required={true}
          helperText={
            <T
              keyName="organization_your_address_to_access_organization"
              params={{
                address: LINKS.ORGANIZATION.buildWithOrigin({
                  [PARAMS.ORGANIZATION_SLUG]: slugValue,
                }),
              }}
            />
          }
        />
      </Box>

      <TextField
        variant="standard"
        data-cy={'organization-description-field'}
        fullWidth
        label={<T keyName="create_organization_description_label" />}
        name="description"
        disabled={disabled}
      />
    </>
  );
};
