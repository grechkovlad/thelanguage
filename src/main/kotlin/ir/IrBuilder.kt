package ir

import Location


class IrBuilder(private val sources: List<ast.SourceFile>) {

    private val classes = mutableMapOf<String, LocatableIrNode<ClassDeclaration>>()
    private val allClassesAst
        get() = sources.flatMap { it.classes }

    fun build(): Project {
        doClassDeclarationlAnalysis()
        doMemberDeclarationAnalysis()
        TODO()
    }

    private fun doMemberDeclarationAnalysis() {
        TODO("Not yet implemented")
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
            is ast.ExpressionStatement -> buildExpressionStatement(statement)
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

    private fun buildExpressionStatement(statement: ast.ExpressionStatement): ExpressionStatement {
        return ExpressionStatement(buildExpression(statement.expression))
    }

    private fun buildExpression(expression: ast.Expression): Expression {
        return when (expression) {
            is ast.ArrayAccess -> TODO()
            is ast.ArrayCreation -> TODO()
            is ast.BinaryOperation -> TODO()
            is ast.ConstructorCall -> TODO()
            is ast.FieldAccess -> TODO()
            is ast.FloatLiteral -> TODO()
            is ast.Identifier -> TODO()
            is ast.IntLiteral -> TODO()
            is ast.MethodCall -> TODO()
            is ast.Null -> TODO()
            is ast.StringLiteral -> TODO()
            is ast.This -> TODO()
            is ast.UnaryOperation -> TODO()
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

    private fun doClassDeclarationlAnalysis() {
        initClassDeclarations()
        analyzeInheritance()
    }


    private fun tryResolveClassRef(name: String, location: Location): ClassReference {
        return systemClassReferences[name] ?: classes[name]?.let { UserClassReference(it.node) }
        ?: throw UnresolvedReference(name, location)
    }

    private fun analyzeInheritance() {
        fillSupertypesReferences()
        checkCyclicInheritance()
    }

    private fun checkCyclicInheritance() {
        val color = mutableMapOf<String, Boolean>()

        fun dfs(node: ClassDeclaration) {
            if (color[node.name] == true) return
            color[node.name] = false
            for (superRef in listOf(node.superClass, *node.interfaces.toTypedArray())) {
                if (color[superRef.name] == false) throw CyclicInheritance(node.name)
                if (superRef is UserClassReference) dfs(superRef.declaration)
            }
            color[node.name] = true
        }
    }

    private fun fillSupertypesReferences() {
        allClassesAst.forEach { clazz ->
            var superClass: ClassReference? = null
            val interfaces = mutableListOf<UserClassReference>()
            for (supertypeIdentifier in clazz.superClasses) {
                val superTypeRef = tryResolveClassRef(supertypeIdentifier.value, supertypeIdentifier.location)
                if (superTypeRef in finalClassesRefs) throw FinalClassSubtyping(
                    supertypeIdentifier.value,
                    supertypeIdentifier.location
                )
                if (!superTypeRef.isInterface) {
                    if (clazz.type == ClassKind.INTERFACE) throw InterfaceInheritsClass(supertypeIdentifier.location)
                    superClass =
                        if (superClass == null) superTypeRef else throw MultipleInheritance(supertypeIdentifier.location)
                } else {
                    interfaces.add(superTypeRef as UserClassReference)
                }
            }
            val currentClassDeclaration = classes[clazz.name.value]!!.node
            currentClassDeclaration.interfaces = interfaces.toList()
            if (currentClassDeclaration.kind == ClassKind.CLASS) {
                currentClassDeclaration.superClass = superClass ?: ObjectClassReference
            }
        }
    }

    private val reservedClassNames = listOf("Object", "String")
    private val finalClassesRefs = listOf(SystemClassReference, StringClassReference)
    private val systemClassReferences = mapOf(
        "Object" to ObjectClassReference,
        "String" to StringClassReference,
        "System" to SystemClassReference
    )

    private fun initClassDeclarations() {
        allClassesAst.forEach { clazz ->
            val name = clazz.name.value
            val location = clazz.name.location
            if (name in reservedClassNames) throw ReservedClassName(name, location)
            if (classes.containsKey(name)) {
                throw NameDeclarationClash(name, classes[name]!!.location, location)
            } else {
                classes[name] = ClassDeclaration(name, clazz.type).withLocation(location)
            }
        }
    }
}

sealed class CompilationError : RuntimeException()

data class NameDeclarationClash(val name: String, val first: Location, val second: Location) :
    CompilationError()

data class ReservedClassName(val name: String, val location: Location) : CompilationError()

data class UnresolvedReference(val name: String, val location: Location) : CompilationError()

data class FinalClassSubtyping(val name: String, val location: Location) : CompilationError()

data class MultipleInheritance(val location: Location) : CompilationError()

data class InterfaceInheritsClass(val location: Location) : CompilationError()

data class CyclicInheritance(val name: String) : CompilationError()

private data class LocatableIrNode<T : IrNode>(val node: T, val location: Location)

private fun <T : IrNode> T.withLocation(location: Location) = LocatableIrNode(this, location)