export function XPathBuilder(initialXpath = '') {
  let xpath = initialXpath;

  function descendant(tag = '*') {
    xpath += `//${tag}`;
    return builder;
  }

  function attributeEquals(attribute: string, value: string) {
    xpath += `[@${attribute}='${value}']`;
    return builder;
  }

  function withAttribute(attribute: string) {
    xpath += `[@${attribute}]`;
    return builder;
  }

  function closestAncestor(tag = '*') {
    xpath += `/ancestor::${tag}`;
    return builder;
  }

  function descendantOrSelf(tag = '*') {
    xpath += `/descendant-or-self::${tag}`;
    return builder;
  }

  function containsText(text: string) {
    xpath += `[contains(text(), '${text}')]`;
    return builder;
  }

  function hasText(text: string) {
    xpath += `[text()='${text}']`;
    return builder;
  }

  function withDataCy(dataCy: DataCy.Value) {
    attributeEquals('data-cy', dataCy);
    return builder;
  }

  function getElement() {
    return cy.xpath(xpath);
  }

  function getInputUnderDataCy(dataCy: DataCy.Value) {
    return builder
      .descendant()
      .withDataCy(dataCy)
      .descendant('input')
      .getElement();
  }

  const builder = {
    descendant,
    attributeEquals,
    closestAncestor,
    descendantOrSelf,
    withDataCy,
    containsText,
    hasText,
    getElement,
    getInputUnderDataCy,
    getXpath: () => xpath,
    withAttribute,
  };

  return builder;
}

export function buildXpath(initialXpath = '') {
  return XPathBuilder(initialXpath);
}
