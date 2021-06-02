import { useFormikContext } from 'formik';
import { useDebounce } from 'use-debounce';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { TextField } from '../../../common/form/fields/TextField';
import { Box, FormHelperText } from '@material-ui/core';
import { LINKS, PARAMS } from '../../../../constants/links';
import { container } from 'tsyringe';
import { OrganizationService } from '../../../../service/OrganizationService';
import { T } from '@tolgee/react';

const organizationService = container.resolve(OrganizationService);

export const OrganizationFields = () => {
  const [addressPartDisabled, setAddressPartDisabled] = useState(true);

  let formik = useFormikContext();
  const [value] = useDebounce(formik.getFieldProps('name').value, 500);
  const addressPartValue = formik.getFieldProps('addressPart').value;

  useEffect(() => {
    const nameMeta = formik.getFieldMeta('name');
    const addressPartMeta = formik.getFieldMeta('addressPart');
    const nameChanged = nameMeta.initialValue !== nameMeta.value;
    //const addressPartChanged = addressPartMeta.initialValue !== addressPartMeta.value

    if (nameChanged) {
      const initialAddressPart =
        formik.getFieldMeta('addressPart').initialValue;
      const addressPartNotTouchedOrEmpty =
        !formik.getFieldMeta('addressPart').touched || addressPartValue === '';
      //autogenerate the addressPart just when not touched and name is valid
      if (
        formik.getFieldMeta('name').error == undefined &&
        value != '' &&
        addressPartNotTouchedOrEmpty
      ) {
        organizationService
          .generateAddressPart(value, initialAddressPart as string)
          .then((addressPart) => {
            formik.getFieldHelpers('addressPart').setValue(addressPart);
            formik.getFieldHelpers('addressPart').setTouched(false);
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
        onClick={() => setAddressPartDisabled(false)}
        style={{ cursor: addressPartDisabled ? 'pointer' : 'initial' }}
      >
        <TextField
          data-cy={'organization-address-part-field'}
          disabled={addressPartDisabled}
          fullWidth
          label={<T>create_organization_addressPart_label</T>}
          name="addressPart"
          required={true}
        />
        <FormHelperText>
          <T
            parameters={{
              address: LINKS.ORGANIZATION.buildWithOrigin({
                [PARAMS.ORGANIZATION_ADDRESS_PART]: addressPartValue,
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
