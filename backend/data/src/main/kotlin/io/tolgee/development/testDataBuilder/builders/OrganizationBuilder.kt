package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole

class OrganizationBuilder(
  val testDataBuilder: TestDataBuilder
) : BaseEntityDataBuilder<Organization, OrganizationBuilder>() {
  class DATA {
    var roles: MutableList<OrganizationRoleBuilder> = mutableListOf()
  }

  val data = DATA()

  fun addRole(ft: FT<OrganizationRole>) = addOperation(data.roles, ft)

  fun addMtCreditBucket(ft: FT<MtCreditBucket>): MtCreditBucketBuilder {
    val builder = MtCreditBucketBuilder()
    testDataBuilder.data.mtCreditBuckets.add(builder)
    builder.self.organization = this@OrganizationBuilder.self
    ft(builder.self)
    return builder
  }

  override var self: Organization = Organization()
}
