enum class ModifierType {
    STATIC, PUBLIC, PRIVATE, PROTECTED, ABSTRACT
}

val ModifierType.isAccessModifier
    get() = this == ModifierType.PUBLIC || this == ModifierType.PROTECTED || this == ModifierType.PRIVATE