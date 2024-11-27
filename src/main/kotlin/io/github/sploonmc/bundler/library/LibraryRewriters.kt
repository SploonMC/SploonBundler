package io.github.sploonmc.bundler.library

object LibraryRewriters {
    val REWRITERS = listOf(VecMathRewriter, )

    object VecMathRewriter : LibraryRewriter {
        override fun isApplicable(dependency: MavenDependency) = dependency.artifactId == "vecmath"

        override fun rewrite(dependency: MavenDependency) = dependency.copy(groupId = "javax.vecmath")
    }

    object LwjglPlatformRewrier : LibraryRewriter {
        override fun isApplicable(dependency: MavenDependency) = dependency.artifactId == "lwjgl-platform"

        override fun rewrite(dependency: MavenDependency) = dependency.copy(groupId = "org.lwjgl")
    }
}