fun readFromResources(path: String) = object {}.javaClass.enclosingClass.getResource(path)!!.readText()
