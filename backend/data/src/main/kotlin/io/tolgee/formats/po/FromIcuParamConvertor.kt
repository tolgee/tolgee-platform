package io.tolgee.formats.po

import com.ibm.icu.text.MessagePatternUtil.ArgNode

interface FromIcuParamConvertor {
  fun convert(node: ArgNode): String
}
