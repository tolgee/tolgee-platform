import { components } from 'tg.service/apiSchema.generated';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { LinkExternal } from 'tg.component/LinkExternal';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

type Props = {
  organizationSlug: string;
  glossary: SimpleGlossaryModel;
} & React.ComponentProps<typeof LinkExternal>;

export const GlossaryLink: React.FC<Props> = ({
  glossary,
  organizationSlug,
  ...props
}) => {
  return (
    <LinkExternal
      component={RouterLink}
      to={LINKS.ORGANIZATION_GLOSSARY.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
        [PARAMS.GLOSSARY_ID]: glossary.id,
      })}
      {...props}
    >
      {glossary.name}
    </LinkExternal>
  );
};
