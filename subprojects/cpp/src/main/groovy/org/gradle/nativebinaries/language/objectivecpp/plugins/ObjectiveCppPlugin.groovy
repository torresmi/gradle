/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.language.objectivecpp.plugins

import org.gradle.api.Action
import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.language.objectivecpp.ObjectiveCppSourceSet
import org.gradle.language.objectivecpp.plugins.ObjectiveCppLangPlugin
import org.gradle.model.ModelRule
import org.gradle.model.ModelRules
import org.gradle.nativebinaries.NativeExecutable
import org.gradle.nativebinaries.NativeLibrary
import org.gradle.nativebinaries.NativeBinary
import org.gradle.nativebinaries.SharedLibraryBinary
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal
import org.gradle.nativebinaries.language.internal.DefaultPreprocessingTool
import org.gradle.nativebinaries.language.objectivecpp.tasks.ObjectiveCppCompile
import org.gradle.nativebinaries.plugins.NativeComponentPlugin
import org.gradle.nativebinaries.toolchain.Clang
import org.gradle.nativebinaries.toolchain.Gcc
import org.gradle.nativebinaries.toolchain.internal.ToolChainRegistryInternal
import org.gradle.nativebinaries.toolchain.internal.ToolType
import org.gradle.nativebinaries.toolchain.internal.tools.DefaultGccCommandLineToolConfiguration

import javax.inject.Inject

/**
 * A plugin for projects wishing to build native binary components from Objective-C++ sources.
 *
 * <p>Automatically includes the {@link ObjectiveCppLangPlugin} for core Objective-C++ support and the {@link NativeComponentPlugin} for native component support.</p>
 *
 * <li>Creates a {@link ObjectiveCppCompile} task for each {@link ObjectiveCppSourceSet} to compile the Objective-C++ sources.</li>
 */
@Incubating
class ObjectiveCppPlugin implements Plugin<ProjectInternal> {

    ModelRules modelRules;

    @Inject
    public ObjectiveCppPlugin(ModelRules modelRules){
        this.modelRules = modelRules
    }

    void apply(ProjectInternal project) {
        project.plugins.apply(NativeComponentPlugin)
        project.plugins.apply(ObjectiveCppLangPlugin)

        modelRules.rule(new ModelRule() {
            void addObjectiveCppCompiler(ToolChainRegistryInternal toolChainRegistry) {
                toolChainRegistry.withType(Clang).all(new Action<Clang>(){
                    void execute(Clang toolchain) {
                        toolchain.add(new DefaultGccCommandLineToolConfiguration("objcppCompiler", ToolType.OBJECTIVECPP_COMPILER, "clang++"));
                    }
                })

                toolChainRegistry.withType(Gcc).all(new Action<Gcc>() {
                    void execute(Gcc toolchain) {
                        toolchain.add(new DefaultGccCommandLineToolConfiguration("objcppCompiler", ToolType.OBJECTIVECPP_COMPILER, "g++"));
                    }
                })
            }
        });


        project.nativeExecutables.all { NativeExecutable executable ->
            executable.binaries.all { binary ->
                binary.extensions.create("objcppCompiler", DefaultPreprocessingTool)
            }
        }

        project.nativeLibraries.all { NativeLibrary library ->
            library.binaries.all { binary ->
                binary.extensions.create("objcppCompiler", DefaultPreprocessingTool)
            }
        }

        project.binaries.withType(NativeBinary) { ProjectNativeBinaryInternal binary ->
            binary.source.withType(ObjectiveCppSourceSet).all { ObjectiveCppSourceSet sourceSet ->
                if (sourceSet.mayHaveSources) {
                    def compileTask = createCompileTask(project, binary, sourceSet)
                    binary.tasks.add compileTask
                    binary.tasks.builder.source compileTask.outputs.files.asFileTree.matching { include '**/*.obj', '**/*.o' }
                }
            }
        }
    }

    private def createCompileTask(ProjectInternal project, ProjectNativeBinaryInternal binary, ObjectiveCppSourceSet sourceSet) {
        def compileTask = project.task(binary.namingScheme.getTaskName("compile", sourceSet.fullName), type: ObjectiveCppCompile) {
            description = "Compiles the $sourceSet of $binary"
        }

        compileTask.toolChain = binary.toolChain
        compileTask.targetPlatform = binary.targetPlatform
        compileTask.positionIndependentCode = binary instanceof SharedLibraryBinary

        compileTask.includes {
            sourceSet.exportedHeaders.srcDirs
        }

        compileTask.source sourceSet.source
        binary.getLibs(sourceSet).each { deps ->
            compileTask.includes deps.includeRoots
        }

        compileTask.objectFileDir = project.file("${project.buildDir}/objectFiles/${binary.namingScheme.outputDirectoryBase}/${sourceSet.fullName}")
        compileTask.macros = binary.objcppCompiler.macros
        compileTask.compilerArgs = binary.objcppCompiler.args

        compileTask
    }

}