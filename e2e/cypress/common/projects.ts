import { gcy } from "./shared";
import { HOST } from "./constants";

export const enterProjectSettings = (projectName: string) => {
  visitList();

  gcy("global-paginated-list")
    .contains(projectName)
    .closest("li")
    .within(() => {
      cy.gcy("project-settings-button").should("be.visible").click();
    });
};

export const enterProject = (projectName: string) => {
  visitList();
  gcy("global-paginated-list").contains(projectName).closest("a").click();
  gcy("global-base-view-content").should("be.visible");
};

export const visitList = () => {
  cy.visit(`${HOST}`);
};
