package ir

val GetField.isStatic: Boolean get() = fieldReference.modifiers.contains(ModifierType.STATIC)

val MethodReference.isStatic: Boolean get() = modifiers.contains(ModifierType.STATIC)

val FieldDeclaration.isStatic: Boolean get() = modifiers.contains(ModifierType.STATIC)

fun UserClassReference.allFields(): Set<FieldReference> = buildSet {
    addAll(this@allFields.declaration.fields.map { it.reference })
    val superClass = declaration.superClass as? UserClassReference ?: return@buildSet
    addAll(superClass.allFields())
}