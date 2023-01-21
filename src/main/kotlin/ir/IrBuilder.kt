package ir

import Location


class IrBuilder(private val sources: List<ast.SourceFile>) {

    private val classes = mutableMapOf<String, ClassDeclaration>()

    private val allClassesAst
        get() = sources.flatMap { it.classes }

    private val ast.ClassDeclaration.fields get() = members.filterIsInstance<ast.FieldDeclaration>()
    private val ast.ClassDeclaration.methods get() = members.filterIsInstance<ast.MethodDeclaration>()

    fun build(): Project {
        doClassDeclarationlAnalysis()
        doMemberDeclarationAnalysis()
        TODO()
    }

    private fun doMemberDeclarationAnalysis() {
        allClassesAst.forEach { classAst ->
            val classIr = classes[classAst.name.value]!!
            doFieldsAnalysis(classAst, classIr)
            doMethodsAnalysis(classAst, classIr)
        }
    }

    private fun doFieldsAnalysis(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        val fieldDeclByName = mutableMapOf<String, FieldDeclaration>()
        val fields = mutableListOf<FieldDeclaration>()
        val nameToLocation = mutableMapOf<String, Location>()
        classAst.fields.forEach { fieldDeclarationAst ->
            val name = fieldDeclarationAst.name.value
            val location = fieldDeclarationAst.name.location
            if (fieldDeclByName.containsKey(name)) {
                throw NameDeclarationClash(name, location, nameToLocation[name]!!)
            }
            val type = resolveType(fieldDeclarationAst.type)
            val fieldDeclarationIr = FieldDeclaration(name, type)
            fields.add(fieldDeclarationIr)
            nameToLocation[name] = location
        }
        classIr.fields = fields
    }

    private val stdlibTypeReferences =
        mapOf(
            "int" to IntTypeReference,
            "float" to FloatTypeReference,
            "bool" to BoolTypeReference,
            "System" to SystemClassReference,
            "String" to StringClassReference,
            "Object" to ObjectClassReference
        )

    private fun resolveType(type: ast.TypeReference): TypeReference {
        when (type) {
            is ast.ArrayTypeReference -> return ArrayTypeReference(resolveType(type.componentTypeReference))
            is ast.SimpleTypeReference -> {
                if (stdlibTypeReferences.containsKey(type.identifier.value)) {
                    return stdlibTypeReferences[type.identifier.value]!!
                }
                classes[type.identifier.value]?.let { return UserClassReference(it) }
                    ?: throw UnresolvedReference(type.identifier.value, type.location)
            }
        }
    }

    private fun doMethodsAnalysis(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
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
        return systemClassReferences[name] ?: classes[name]?.let { UserClassReference(it) }
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
            val currentClassDeclaration = classes[clazz.name.value]!!
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
        val nameToLocation = mutableMapOf<String, Location>()
        allClassesAst.forEach { clazz ->
            val name = clazz.name.value
            val location = clazz.name.location
            if (name in reservedClassNames) throw ReservedClassName(name, location)
            if (classes.containsKey(name)) {
                throw NameDeclarationClash(name, nameToLocation[name]!!, location)
            } else {
                val declaration = ClassDeclaration(name, clazz.type)
                classes[name] = declaration
                nameToLocation[name] = location
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