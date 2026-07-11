/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.authentication

// /**
// * Enables callee to authenticate using a relevant API keys/tokens.
// */
// @Parameter(
//  `in` = ParameterIn.QUERY,
//  name = "ak",
//  style = ParameterStyle.FORM,
//  schema = Schema(type = "string"),
//  explode = Explode.TRUE,
//  example = "tgpak_gm2dcxzynjvdqm3fozwwgmdjmvwdgojqonvxamldnu4hi5lp",
//  description = "API key provided via query parameter. Will be deprecated in the future.",
// )
// @Parameter(
//  `in` = ParameterIn.HEADER,
//  name = API_KEY_HEADER_NAME,
//  style = ParameterStyle.FORM,
//  schema = Schema(type = "string"),
//  explode = Explode.TRUE,
//  example = "tgpak_gm2dcxzynjvdqm3fozwwgmdjmvwdgojqonvxamldnu4hi5lp",
//  description = "API key provided via header. Safer since headers are not stored in server logs.",
// )
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AllowApiAccess(
  val tokenType: AuthTokenType = AuthTokenType.ANY,
)
