package ir

import ast.FileRelativeLocation
import ast.SourceFile


class IrBuilder(private val sources: List<SourceFile>) {
    fun build(): Project {
        checkClassDeclarationClash()
        val classes = mutableListOf<ClassDeclaration>()
        sources.forEach { source -> source.classes.forEach { classes.add(build(it)) } }
        return Project(classes)
    }

    private fun build(classDeclarationAst: ast.ClassDeclaration): ClassDeclaration {
        val members = mutableListOf<Member>()
        classDeclarationAst.members.forEach { member -> members.add(buildMember(member)) }
        return ClassDeclaration(classDeclarationAst.type, members)
    }

    private fun buildMember(memberAst: ast.MemberDeclaration): Member {
        return when (memberAst) {
            is ast.FieldDeclaration -> buildFieldDeclaration(memberAst)
            is ast.ConstructorDeclaration -> buildConstructorDeclaration(memberAst)
            is ast.MethodDeclaration -> buildMethodDeclaration(memberAst)
            is ast.StaticInitBlock -> buildStaticInitBlock(memberAst)
        }
    }

    private fun buildStaticInitBlock(staticInitBlock: ast.StaticInitBlock): StaticInitBlock {
        val statements = staticInitBlock.body.statements.map { buildStatement(it) }
        return StaticInitBlock(statements)
    }

    private fun buildStatement(statement: ast.Statement): Statement {
        return when (statement) {
            is ast.Assignment -> TODO()
            is ast.ExpressionStatement -> TODO()
            is ast.ForStatement -> TODO()
            is ast.IfStatement -> TODO()
            is ast.ConstructorDeclaration -> TODO()
            is ast.FieldDeclaration -> TODO()
            is ast.MethodDeclaration -> TODO()
            is ast.StaticInitBlock -> TODO()
            is ast.ReturnStatement -> TODO()
            is ast.SuperCall -> TODO()
            is ast.VariableDeclaration -> TODO()
            is ast.WhileStatement -> TODO()
        }
    }

    private fun buildMethodDeclaration(methodDeclaration: ast.MethodDeclaration): MethodDeclaration {
        TODO("Not yet implemented")
    }

    private fun buildConstructorDeclaration(constructorDeclaration: ast.ConstructorDeclaration): ConstructorDeclaration {
        TODO("Not yet implemented")
    }

    private fun buildFieldDeclaration(fieldDeclaration: ast.FieldDeclaration): FieldDeclaration {
        TODO("Not yet implemented")
    }

    private fun checkClassDeclarationClash() {
        val fistDeclaration = mutableMapOf<String, AbsoluteLocation>()
        sources.forEach { source : SourceFile ->
            source.classes.forEach { clazz ->
                val name = clazz.name.value
                val location = AbsoluteLocation(source.file, clazz.name.location)
                if (fistDeclaration.containsKey(name)) {
                    throw NameDeclarationClash(name, fistDeclaration[name]!!, location)
                } else {
                    fistDeclaration[name] = location
                }
            }
        }
    }

}

sealed class CompilationError : RuntimeException()

data class NameDeclarationClash(val name: String, val first: AbsoluteLocation, val second: AbsoluteLocation) :
    CompilationError()

data class AbsoluteLocation(val file: String, val inFileLocation: FileRelativeLocation)