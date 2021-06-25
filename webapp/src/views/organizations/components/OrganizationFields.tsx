import { useEffect, useState } from 'react';
import { Box, FormHelperText } from '@material-ui/core';
import { T } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { container } from 'tsyringe';
import { useDebounce } from 'use-debounce';

import { TextField } from 'tg.component/common/form/fields/TextField';
import { LINKS, PARAMS } from 'tg.constants/links';
import { OrganizationService } from 'tg.service/OrganizationService';

const organizationService = container.resolve(OrganizationService);

export const OrganizationFields = () => {
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
        data-cy={'organization-name-field'}
        fullWidth
        label={<T>create_organization_name_label</T>}
        name="name"
        required={true}
      />
      <Box
        onClick={() => setSlugDisabled(false)}
        style={{ cursor: slugDisabled ? 'pointer' : 'initial' }}
      >
        <TextField
          data-cy={'organization-address-part-field'}
          disabled={slugDisabled}
          fullWidth
          label={<T>create_organization_slug_label</T>}
          name="slug"
          required={true}
        />
        <FormHelperText>
          <T
            parameters={{
              address: LINKS.ORGANIZATION.buildWithOrigin({
                [PARAMS.ORGANIZATION_SLUG]: slugValue,
              }),
            }}
          >
            organization_your_address_to_access_organization
          </T>
        </FormHelperText>
      </Box>

      <TextField
        data-cy={'organization-description-field'}
        fullWidth
        label={<T>create_organization_description_label</T>}
        name="description"
      />
    </>
  );
};
