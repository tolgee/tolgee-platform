import { Box, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { actionsConfiguration, entitiesConfiguration } from './configuration';
import {
  DiffValue,
  EntityOptions,
  FieldOptions,
  FieldOptionsObj,
} from './types';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];
type ModifiedEntityModel = components['schemas']['ModifiedEntityModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 50px 1fr;
`;

const formatValue = (value: any, options: FieldOptions) => {
  switch (typeof value) {
    case 'string':
      return value;
    case 'number':
      return String(value);
    case 'boolean':
      return String(value);
  }
};

const notEmpty = (value?: any) => {
  return value !== null && value !== undefined;
};

const formatDiff = (value: DiffValue<any>, options: FieldOptions) => {
  if (notEmpty(value.old) && notEmpty(value.new)) {
    return formatValue(value.new, options);
  } else if (notEmpty(value.old)) {
    return formatValue(value.old, options);
  } else if (notEmpty(value.new)) {
    return formatValue(value.new, options);
  }
};

const formatField = (
  value: DiffValue<any>,
  options: FieldOptions,
  name: string
) => {
  const optionsObj = (
    typeof options === 'object' ? options : {}
  ) as FieldOptionsObj;
  const label = optionsObj?.label ? optionsObj?.label || name : null;
  if (label) {
    return (
      <>
        <Box gridArea="label">{label}</Box>
        <Box>{formatDiff(value, options)}</Box>
      </>
    );
  } else {
    return <Box gridColumn="1 / span 2">{formatDiff(value, options)}</Box>;
  }
};

const formatEntity = (
  entityData: ModifiedEntityModel,
  options: EntityOptions
) => {
  const description = options.description?.(entityData);
  return (
    <>
      {description && <Box gridColumn="1 / span 2">{description}</Box>}
      {Object.entries(options.fields).map(([fieldName, o]) => {
        const fieldData = entityData.modifications?.[fieldName];
        if (fieldData) {
          return <>{formatField(fieldData, o, fieldName)}</>;
        }
      })}
    </>
  );
};

const formatAction = (data: ProjectActivityModel) => {
  const config = actionsConfiguration[data.type];
  const description = config?.description?.(data);

  return (
    <>
      {description && <Box gridColumn="1 / span 2">{description}</Box>}
      {data.modifiedEntities &&
        Object.entries(data?.modifiedEntities)
          .filter(([entityName]) => config?.entities?.includes(entityName))
          .map(([entityName, value]) => {
            return (
              <>
                {value.map((entity) => {
                  const options = entitiesConfiguration[
                    entityName
                  ] as EntityOptions;
                  if (options) {
                    return <>{formatEntity(entity, options)}</>;
                  }
                })}
              </>
            );
          })}
    </>
  );
};

type Props = {
  data: ProjectActivityModel;
};

export const ActivityConfigurable = ({ data }: Props) => {
  return (
    <StyledContainer>
      {formatAction(data)}
      {/* <pre>{JSON.stringify(data, null, 2)}</pre> */}
    </StyledContainer>
  );
};
