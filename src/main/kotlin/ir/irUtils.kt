package ir

val GetField.isStatic: Boolean get() = fieldReference.modifiers.contains(ModifierType.STATIC)

val MethodReference.isStatic: Boolean get() = modifiers.contains(ModifierType.STATIC)

val MethodReference.isAbstract: Boolean get() = modifiers.contains(ModifierType.ABSTRACT)

val FieldDeclaration.isStatic: Boolean get() = modifiers.contains(ModifierType.STATIC)

val FieldReference.isStatic: Boolean get() = modifiers.contains(ModifierType.STATIC)

fun UserClassReference.allFields(): Set<FieldReference> = buildSet {
    addAll(this@allFields.declaration.fields.map { it.reference })
    val superClass = declaration.superClass as? UserClassReference ?: return@buildSet
    addAll(superClass.allFields())
}

fun TypeReference.isSubtypeOf(other: TypeReference): Boolean = when (this) {
    is ArrayTypeReference -> this == other || other == ObjectClassReference
    BoolTypeReference -> this == other
    ObjectClassReference -> this == other
    StringClassReference -> this == other || other == ObjectClassReference
    SystemClassReference -> this == other || other == ObjectClassReference
    is UserClassReference -> this == other || declaration.superClass.isSubtypeOf(other) || declaration.interfaces.any {
        it.isSubtypeOf(other)
    }

    FloatTypeReference -> this == other
    IntTypeReference -> this == other
    NullTypeReference -> other.isSubtypeOf(ObjectClassReference)
    VoidTypeReference -> this == other
    UtilsClassReference -> this == other
}

val ClassReference.superClass: ClassReference
    get() = when (this) {
        ObjectClassReference -> error("Object has no superclass")
        StringClassReference -> ObjectClassReference
        SystemClassReference -> ObjectClassReference
        is UserClassReference -> this.declaration.superClass
        UtilsClassReference -> ObjectClassReference
    }