package io.tolgee.gradle

plugins {
    id("org.liquibase.gradle")
}

project.extensions.add("configureLiquibase", { schema: String, referenceUrlPrefix: String, changeLogPah: String ->
    configure<org.liquibase.gradle.LiquibaseExtension> {
        activities.register("main") {
            this.arguments = mapOf(
                    "changeLogFile" to changeLogPah,
                    "url" to "jdbc:postgresql://localhost:55432/postgres?currentSchema=$schema",
                    "referenceUrl" to referenceUrlPrefix +
                            "?dialect=io.tolgee.dialects.postgres.CustomPostgreSQLDialect" +
                            "&hibernate.physical_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy" +
                            "&hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy",
                    "username" to "postgres",
                    "password" to "postgres",
                    "driver" to "org.postgresql.Driver",
                    "excludeObjects" to "table:batch_job_execution_context," +
                            "batch_step_execution_seq," +
                            "batch_job_seq," +
                            "batch_job_execution_seq," +
                            "batch_step_execution_context," +
                            "batch_step_execution," +
                            "batch_job_instance," +
                            "table:batch_job_execution," +
                            "table:batch_job_execution_params," +
                            "hibernate_sequence," +
                            "revision_sequence_generator," +
                            "billing_sequence," +
                            "activity_sequence," +
                            "FK9xs5a07fba5yqje5jqm6qrehs," +
                            "column:textsearchable_.*"
            )
        }
    }
})
