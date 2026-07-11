import { components } from 'tg.service/apiSchema.generated';
import React, {
  ComponentProps,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import Box from '@mui/material/Box';
import { InputBaseComponentProps, styled } from '@mui/material';
import { useField, useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { SelectItem } from 'tg.component/searchSelect/SelectItem';
import { InfiniteSearchSelect } from 'tg.component/searchSelect/InfiniteSearchSelect';
import { LanguageValue } from 'tg.component/languages/LanguageValue';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const StyledPlaceholder = styled('span')`
  color: ${({ theme }) => theme.palette.tokens.text.tertiary};
`;

const StyledFlaggedInput = styled('div')`
  display: inline-flex;
  align-items: center;
  padding: 8.5px 14px;
  height: 23px;
  box-sizing: content-box;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  /* line-height: 1 collapses the text box to glyph height so its visual centre lines up
     with the centre of the flag image (default line-height leaves descender space below
     that shifts the visual centre downward). */
  line-height: 1;

  & > .tm-flag {
    margin-left: ${({ theme }) => theme.spacing(2)};
    display: inline-flex;
    align-items: center;
    /* Flex centres the flag image against the full line-box; visually the glyph mass of
       a lowercase-heavy word like "English" sits below the geometric centre, so we nudge
       the flag down a touch to make the centres line up visually. */
    transform: translateY(2px);
  }
`;

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];
type SelectedLanguageModel = {
  tag: string;
  name: string;
  flagEmoji?: string;
};

type Props = {
  name: string;
  assignedProjectsName?: string;
  disabled?: boolean;
  label?: React.ReactNode;
  minHeight?: boolean;
  autoSelectFirst?: boolean;
} & Omit<ComponentProps<typeof Box>, 'children' | 'minHeight'>;

export const BaseLanguageSelect: React.VFC<Props> = ({
  name,
  assignedProjectsName,
  disabled,
  label,
  minHeight,
  autoSelectFirst = true,
  ...boxProps
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const context = useFormikContext();
  const { t } = useTranslate();
  const [field, meta] = useField(name);
  const value = field.value as SelectedLanguageModel | undefined;

  // meta.error shape depends on which Yup rule fired:
  //   - object-level `.required()` (field cleared / undefined) → string
  //   - shape `{ tag: ... }` validation → { tag: string }
  //   - localized message → ReactElement
  const error = React.isValidElement(meta.error)
    ? meta.error
    : typeof meta.error === 'string'
    ? meta.error
    : (meta.error as any)?.tag;

  const showError = (meta.touched || context.submitCount > 0) && error;

  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, 500);

  // For filtering available languages by assigned projects
  const assignedProjects = assignedProjectsName
    ? context.getFieldProps(assignedProjectsName)?.value
    : undefined;
  const assignedProjectIds = assignedProjects?.map((p) => p.id);

  const query = {
    projectIds: assignedProjectIds,
    search: searchDebounced,
    size: 30,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/base-languages',
    method: 'get',
    path: { organizationId: preferredOrganization!.id },
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: preferredOrganization!.id },
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const data = dataLoadable.data?.pages.flatMap(
    (p) => p._embedded?.languages ?? []
  );

  // Auto-select the first language when no value is set and data is loaded.
  // The ref prevents re-triggering after setSelected updates the Formik field.
  const hasAutoSelected = useRef(false);
  useEffect(() => {
    if (
      autoSelectFirst &&
      !value &&
      data &&
      data.length > 0 &&
      !hasAutoSelected.current
    ) {
      hasAutoSelected.current = true;
      setSelected(data[0]);
    }
  }, [value, data]);

  const handleFetchMore = () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      dataLoadable.fetchNextPage();
    }
  };

  const setValue = (v?: SelectedLanguageModel) =>
    context.setFieldValue(name, v);
  const setSelected = (item: OrganizationLanguageModel) => {
    const itemSelected: SelectedLanguageModel = {
      tag: item.tag,
      name: item.name,
      flagEmoji: item.flagEmoji,
    };
    setValue(itemSelected);
  };

  function renderItem(props: object, item: OrganizationLanguageModel) {
    const selected = value?.tag === item.tag;
    return (
      <SelectItem
        {...props}
        data-cy="base-language-select-item"
        selected={selected}
        label={<LanguageValue language={item} />}
        onClick={() => setSelected(item)}
      />
    );
  }

  function labelItem(item: SelectedLanguageModel) {
    return item.name;
  }

  // Reads the live selected language at render time so it can paint the SVG flag next to
  // the name in the selected-value display. Stable identity (defined once) avoids the
  // MUI input losing focus on every re-render.
  const selectedRef = useRef<SelectedLanguageModel | undefined>(value);
  selectedRef.current = value;
  const FlaggedInput = useMemo(
    () =>
      React.forwardRef<HTMLDivElement, InputBaseComponentProps>(
        function FlaggedInput(
          { value: inputValue, placeholder, ...rest },
          ref
        ) {
          const flag = selectedRef.current?.flagEmoji;
          return (
            <StyledFlaggedInput tabIndex={0} ref={ref} {...(rest as any)}>
              <span>
                {inputValue || (
                  <StyledPlaceholder>{placeholder}</StyledPlaceholder>
                )}
              </span>
              {flag && (
                <span className="tm-flag">
                  <FlagImage flagEmoji={flag} width={20} />
                </span>
              )}
            </StyledFlaggedInput>
          );
        }
      ),
    []
  );

  return (
    <Box data-cy="base-language-select" {...boxProps}>
      <InfiniteSearchSelect
        items={data}
        selected={value}
        queryResult={dataLoadable}
        itemKey={(item) => item.tag}
        search={search}
        onClearSelected={() => setValue(undefined)}
        onSearchChange={setSearch}
        onFetchMore={handleFetchMore}
        renderItem={renderItem}
        labelItem={labelItem}
        inputComponent={FlaggedInput}
        label={label ?? t('field_base_language', 'Base language')}
        error={showError}
        searchPlaceholder={t('language_search_placeholder')}
        disabled={disabled}
        minHeight={minHeight}
      />
    </Box>
  );
};
