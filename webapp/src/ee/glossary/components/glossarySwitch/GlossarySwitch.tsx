import { useRef, useState } from 'react';
import { Box, Link, styled } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { GlossaryPopover } from './GlossaryPopover';
import { GlossaryItem } from './GlossaryItem';
import { components } from 'tg.service/apiSchema.generated';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

const StyledLink = styled(Link)`
  display: flex;
`;

export const GlossarySwitch: React.FC = () => {
  const anchorEl = useRef<HTMLAnchorElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();
  const history = useHistory();

  const handleClose = () => {
    setIsOpen(false);
  };

  const handleClick = () => {
    setIsOpen(true);
  };

  const handleSelectGlossary = (selected: SimpleGlossaryModel) => {
    handleClose();
    history.push(
      LINKS.ORGANIZATION_GLOSSARY.build({
        [PARAMS.GLOSSARY_ID]: selected.id,
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
      })
    );
  };

  if (!preferredOrganization) {
    return null;
  }

  return (
    <>
      <Box display="flex" data-cy="glossary-switch" overflow="hidden">
        <StyledLink
          ref={anchorEl}
          style={{
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            flexShrink: 1,
          }}
          onClick={handleClick}
        >
          <GlossaryItem data={glossary} />
          <ArrowDropDown
            width={20}
            height={20}
            style={{ marginRight: '-6px' }}
          />
        </StyledLink>

        <GlossaryPopover
          open={isOpen}
          onClose={handleClose}
          selectedId={glossary.id}
          onSelect={handleSelectGlossary}
          anchorEl={anchorEl.current!}
          organizationId={preferredOrganization.id}
        />
      </Box>
    </>
  );
};
