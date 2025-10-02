import React from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Chip, styled } from '@mui/material';
import { InfiniteSearchSelect } from 'tg.component/searchSelect/InfiniteSearchSelect';
import { BranchLabel } from 'tg.component/branching/BranchLabel';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { SelectItem } from 'tg.component/searchSelect/SelectItem';

type BranchModel = components['schemas']['BranchModel'];

const StyledLabel = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const BranchSelector = () => {
  const { t } = useTranslate();
  const project = useProject();
  const history = useHistory();
  const {
    selected,
    available: branches,
    loadable,
  } = useTranslationsSelector((c) => c.branches);

  if (!loadable.isLoading && !selected) {
    history.replace(
      LINKS.PROJECT_TRANSLATIONS.build({
        [PARAMS.PROJECT_ID]: project.id,
      })
    );
  }

  function changeBranch(item: BranchModel) {
    history.replace(
      LINKS.PROJECT_TRANSLATIONS_BRANCHED.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.TRANSLATIONS_BRANCH]: item.name,
      })
    );
  }

  function renderItem(props: any, item: BranchModel) {
    return (
      <SelectItem
        {...props}
        label={
          <StyledLabel>
            <div>{item.name}</div>
            {item.isDefault && (
              <Chip size={'small'} label={t('default_branch')} />
            )}
          </StyledLabel>
        }
        selected={item.id === selected?.id}
        onClick={() => changeBranch(item)}
      />
    );
  }

  return (
    <Box display="grid">
      <InfiniteSearchSelect
        items={branches}
        queryResult={loadable}
        itemKey={(item) => item.id}
        selected={selected!}
        minHeight={false}
        search={''}
        renderItem={renderItem}
        labelItem={(item: BranchModel) => {
          return item?.name;
        }}
        inputComponent={BranchLabel}
        menuAnchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
      />
    </Box>
  );
};
