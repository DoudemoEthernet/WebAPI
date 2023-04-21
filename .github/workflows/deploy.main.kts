#!/usr/bin/env kotlin
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:0.41.0")

import io.github.typesafegithub.workflows.actions.actions.CheckoutV3
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.actions.CustomAction
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.Contexts
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.toYaml

val CLOUDFLARE_API_TOKEN by Contexts.secrets
val CLOUDFLARE_ACCOUNT_ID by Contexts.secrets

val targetBranches = listOf("main")
val targetPaths = listOf("v1.yaml", ".github/workflows/deploy.yaml")
val workflow = workflow(
    name = "Deploy API",
    on = listOf(
        Push(
            branches = targetBranches,
            paths = targetPaths
        ),
        PullRequest(
            branches = targetBranches,
            paths = targetPaths
        )
    ),
    sourceFile = __FILE__.toPath()
) {
    job(
        id = "deploy",
        runsOn = UbuntuLatest,
    ) {
        uses(name = "Check out", action = CheckoutV3())
        run(
            name = "Generate Static site",
            command = """
            curl -L -O https://github.com/swagger-api/swagger-ui/archive/refs/heads/master.zip
            7z x master.zip
            cp -r swagger-ui-master/dist out
            sed -i 's|https://petstore.swagger.io/v2/swagger.json|https://doudemoethernet-webapi.pages.dev/api/v1.yaml|' out/swagger-initializer.js
            mkdir -p out/api
            cp v1.yaml out/api
            """.trimIndent()
        )
        uses(
            name = "Publish to Cloudflare Pages",
            action = CustomAction(
                actionOwner = "cloudflare",
                actionName = "pages-action",
                actionVersion = "v1",
                inputs = mapOf(
                    "apiToken" to expr { CLOUDFLARE_API_TOKEN },
                    "accountId" to expr { CLOUDFLARE_ACCOUNT_ID },
                    "projectName" to "doudemoethernet-webapi",
                    "directory" to "out",
                    "gitHubToken" to expr { secrets.GITHUB_TOKEN }
                )
            )
        )
    }
}

println(workflow.toYaml())