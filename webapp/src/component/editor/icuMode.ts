import { rules } from './icuHighlightRules';

export class IcuHighlightRules extends ace.require(
  'ace/mode/text_highlight_rules'
).TextHighlightRules {
  constructor() {
    super();

    this.$rules = rules;
  }
}

export default class IcuMode extends ace.require('ace/mode/text').Mode {
  constructor() {
    super();
    this.HighlightRules = IcuHighlightRules;
  }
}
