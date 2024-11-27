package io.github.sploonmc.bundler.library

interface LibraryRewriter {
    fun isApplicable(dependency: MavenDependency): Boolean

    fun rewrite(dependency: MavenDependency): MavenDependency
}