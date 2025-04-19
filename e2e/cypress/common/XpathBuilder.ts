export function XPathBuilder(initialXpath = '') {
  const xpath = initialXpath;

  function descendant(tag = '*') {
    return XPathBuilder(`${xpath}//${tag}`);
  }

  function attributeEquals(attribute: string, value: string) {
    return XPathBuilder(`${xpath}[@${attribute}='${value}']`);
  }

  function withAttribute(attribute: string) {
    return XPathBuilder(`${xpath}[@${attribute}]`);
  }

  function closestAncestor(tag = '*') {
    return XPathBuilder(`${xpath}/ancestor::${tag}`);
  }

  function descendantOrSelf(tag = '*') {
    return XPathBuilder(`${xpath}/descendant-or-self::${tag}`);
  }

  function containsText(text: string) {
    return XPathBuilder(`${xpath}[contains(text(), '${text}')]`);
  }

  function hasText(text: string) {
    return XPathBuilder(`${xpath}[text()='${text}']`);
  }

  function withDataCy(dataCy: DataCy.Value) {
    return attributeEquals('data-cy', dataCy);
  }

  function getElement() {
    return cy.xpath(xpath);
  }

  function getInputUnderDataCy(dataCy: DataCy.Value) {
    return descendant().withDataCy(dataCy).descendant('input').getElement();
  }

  function nth(nth: number) {
    return XPathBuilder(`(${xpath})[${nth}]`);
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
    nth,
  };

  return builder;
}

export function buildXpath(initialXpath = '') {
  return XPathBuilder(initialXpath);
}

export type XpathBuilderType = ReturnType<typeof XPathBuilder>;
