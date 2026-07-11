import { gcy } from '../../../common/shared';

type PaletteColor = {
  index: number;
  hex: string;
};

export class E2LabelModal {
  fill({
    name,
    color,
    description,
  }: {
    name: string;
    color?: string | PaletteColor;
    description?: string;
  }) {
    cy.get('input[name="name"]').clear().type(name);

    if (typeof color === 'string') {
      cy.get('input[name="color"]').then(($input) => {
        cy.wrap($input).clear().type(color);
      });
      gcy('color-preview').should(
        'have.css',
        'background-color',
        `rgb(${this.hexToRgb(color)})`
      );
    } else {
      gcy('color-preview')
        .click()
        .then(() => {
          gcy('color-palette-popover')
            .should('be.visible')
            .within(() => {
              gcy('palette-color').eq(color.index).click();
            });
        });
    }

    if (description) {
      cy.get('textarea[name="description"]').clear().type(description);
    }
  }

  save() {
    gcy('global-form-save-button').click();
  }

  fillAndSave(
    name: string,
    color: string | PaletteColor,
    description?: string
  ) {
    this.fill({ name, color, description });
    this.save();
  }

  assertDefaultColorIsFilled() {
    cy.get('input[name="color"]')
      .invoke('val')
      .should('match', new RegExp('^#[A-Fa-f0-9]{6}$'));
  }

  private hexToRgb(hex: string): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `${r}, ${g}, ${b}`;
  }
}
