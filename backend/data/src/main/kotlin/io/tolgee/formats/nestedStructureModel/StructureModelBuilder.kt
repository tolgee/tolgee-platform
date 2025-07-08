package io.tolgee.formats.nestedStructureModel

import io.tolgee.formats.path.ArrayPathItem
import io.tolgee.formats.path.ObjectPathItem
import io.tolgee.formats.path.PathItem
import io.tolgee.formats.path.buildPath
import io.tolgee.formats.path.getPathItems

class StructureModelBuilder(
  private var structureDelimiter: Char?,
  private var supportArrays: Boolean,
  private val rootKeyIsLanguageTag: Boolean = false,
) {
  private var model: StructuredModelItem? = null

  val result: Any?
    get() {
      return model?.toMapOrList()
    }

  private fun StructuredModelItem.toMapOrList(): Any? {
    return when (this) {
      is ObjectStructuredModelItem ->
        mutableMapOf<String, Any?>().also { theMap ->
          this.forEach { (key, value) ->
            theMap[key] = value.toMapOrList()
          }
        }

      is ArrayStructuredModelItem ->
        mutableListOf<Any?>().also { theList ->
          this.entries.sortedBy { it.key }.forEach {
            theList.add(it.value.toMapOrList())
          }
        }

      is ValueStructuredModelItem -> this.value
      else -> throw IllegalStateException("Uknown model type")
    }
  }

  fun addValue(
    languageTag: String,
    key: String,
    value: String?,
  ) {
    val path = getPathItems(languageTag, key)
    addValueToPath(path, value)
  }

  fun addValue(
    languageTag: String,
    key: String,
    pluralForms: Map<String, String>,
  ) {
    pluralForms.forEach { (keyword, form) ->
      val path = getPathItems(languageTag, key) + ObjectPathItem(keyword, keyword)
      addValueToPath(path, form)
    }
  }

  private fun getPathItems(
    languageTag: String,
    key: String,
  ) = getLanguageTagPath(languageTag) + getPathItems(key, supportArrays, structureDelimiter)

  private fun addValueToPath(
    path: List<PathItem>,
    value: String?,
  ) {
    model = model ?: path.first().createNode(null, null)
    addToContent(model!!, path, path, value)
  }

  private fun getLanguageTagPath(languageTag: String): MutableList<PathItem> {
    if (rootKeyIsLanguageTag) {
      return mutableListOf(ObjectPathItem(languageTag, languageTag))
    }
    return mutableListOf()
  }

  private fun addToContent(
    parentNode: StructuredModelItem,
    pathItems: List<PathItem>,
    fullPath: List<PathItem>,
    value: String?,
  ) {
    val pathItemsMutable = pathItems.toMutableList()

    if (pathItems.size == 1) {
      putText(parentNode, value, pathItems.first(), fullPath)
      return
    }

    val pathItem = pathItemsMutable.removeAt(0)

    if (parentNode is ValueStructuredModelItem) {
      handleCollisions(pathItem, parentNode, fullPath, value)
    }

    when (pathItem) {
      is ObjectPathItem -> {
        when (parentNode) {
          is ObjectStructuredModelItem -> {
            val targetNode =
              getTargetNodeForObjectItem(parentNode, pathItem, pathItemsMutable) ?: return
            addToContent(targetNode, pathItemsMutable, fullPath, value)
          }

          is ArrayStructuredModelItem -> {
            handleCollisions(pathItem, parentNode, fullPath, value)
          }
        }
      }

      is ArrayPathItem -> {
        when (parentNode) {
          is ArrayStructuredModelItem -> {
            val targetNode =
              getTargetNodeForArrayItem(parentNode, pathItem, pathItemsMutable) ?: return
            addToContent(targetNode, pathItemsMutable, fullPath, value)
          }

          is ObjectStructuredModelItem -> {
            handleCollisions(pathItem, parentNode, fullPath, value)
          }
        }
      }
    }
  }

  private fun getTargetNodeForObjectItem(
    parentNode: ObjectStructuredModelItem,
    currentPathItem: ObjectPathItem,
    restPathItems: MutableList<PathItem>,
  ): StructuredModelItem {
    var targetNode = parentNode[currentPathItem.key]
    if (targetNode == null) {
      targetNode = restPathItems.first().createNode(parentNode, currentPathItem.key)
      parentNode[currentPathItem.key] = targetNode
    }
    return targetNode
  }

  private fun getTargetNodeForArrayItem(
    parentNode: ArrayStructuredModelItem,
    currentPathItem: ArrayPathItem,
    restPathItems: MutableList<PathItem>,
  ): StructuredModelItem {
    var targetNode = parentNode[currentPathItem.index]
    if (targetNode == null) {
      targetNode = restPathItems.first().createNode(parentNode, currentPathItem.index)
      parentNode[currentPathItem.index] = targetNode
      return targetNode
    }

    return targetNode
  }

  private fun putText(
    parentNode: StructuredModelItem,
    text: String?,
    pathItem: PathItem,
    fullPath: List<PathItem>,
  ) {
    handleCollisions(pathItem, parentNode, fullPath, text)

    if (pathItem is ObjectPathItem && parentNode is ObjectStructuredModelItem) {
      parentNode.compute(pathItem.key) { _, value ->
        throwIfExists(value, fullPath)
        ValueStructuredModelItem(text, parentNode, pathItem.key)
      }
    }

    if (pathItem is ArrayPathItem && parentNode is ArrayStructuredModelItem) {
      parentNode.compute(pathItem.index) { _, value ->
        throwIfExists(value, fullPath)
        ValueStructuredModelItem(text, parentNode, pathItem.index)
      }
    }
  }

  private fun handleCollisions(
    pathItem: PathItem,
    parentNode: StructuredModelItem,
    fullPath: List<PathItem>,
    text: String?,
  ) {
    when (pathItem) {
      is ObjectPathItem -> {
        when (parentNode) {
          is ArrayStructuredModelItem -> {
            handleRootIsArrayCollision(parentNode, fullPath, text)
          }

          is ValueStructuredModelItem -> {
            handleExistingNodeCollisionByJoiningLast2PathSegments(parentNode, fullPath, text)
          }
        }
      }

      is ArrayPathItem -> {
        when (parentNode) {
          is ObjectStructuredModelItem -> {
            handleExistingNodeCollisionByConvertingArrayItemToObjectItem(pathItem, fullPath, text)
          }

          is ValueStructuredModelItem -> {
            handleExistingNodeCollisionByJoiningLast2PathSegments(parentNode, fullPath, text)
          }
        }
      }
    }
  }

  private fun throwIfExists(
    value: StructuredModelItem?,
    fullPath: List<PathItem>,
  ) {
    if (value != null) {
      throw IllegalStateException(
        "Cannot add item to node. This is a bug, data should be sorted by key name path. Path: ${
          buildPath(
            fullPath,
          )
        }",
      )
    }
  }

  private fun PathItem.createNode(
    parentNode: ContainerNode<*>?,
    key: Any?,
  ): StructuredModelItem {
    return when (this) {
      is ArrayPathItem -> ArrayStructuredModelItem(parentNode, key)
      is ObjectPathItem -> ObjectStructuredModelItem(parentNode, key)
      else -> throw IllegalStateException("Root item must be array or object")
    }
  }

  private fun handleExistingNodeCollisionByJoiningLast2PathSegments(
    parentNode: StructuredModelItem,
    fullPath: List<PathItem>,
    value: String?,
  ) {
    // we can only use different index
    val parent = parentNode.parent
    if (parent is ArrayStructuredModelItem) {
      handleExistingNodeCollisionByIncreasingIndex(parent, fullPath, value)
      return
    }

    // for objects, we can basically flatten the last 2 path items
    val last2joined = buildPath(fullPath.takeLast(2), structureDelimiter)
    val joinedPathItems = fullPath.dropLast(2) + ObjectPathItem(last2joined, last2joined)
    addToContent(model!!, joinedPathItems, joinedPathItems, value)
  }

  private fun handleExistingNodeCollisionByConvertingArrayItemToObjectItem(
    pathItem: ArrayPathItem,
    fullPath: List<PathItem>,
    text: String?,
  ) {
    val fullPathMutable = fullPath.toMutableList()
    val index = fullPath.indexOf(pathItem)
    fullPathMutable[index] = ObjectPathItem("[${pathItem.index}]", "[${pathItem.index}]")
    addToContent(model!!, fullPathMutable, fullPath, text)
  }

  /**
   * since the parent is already an array, we cannot convert it to an object
   * so the only option is to use another index
   */
  private fun handleExistingNodeCollisionByIncreasingIndex(
    node: ArrayStructuredModelItem,
    fullPath: List<PathItem>,
    value: String?,
  ) {
    val newIndex = node.keys.maxOrNull()?.plus(1) ?: 0
    val arrayItem = fullPath.getOrNull(fullPath.size - 2) ?: return
    (arrayItem as? ArrayPathItem)?.index = newIndex
    addToContent(model!!, fullPath, fullPath, value)
  }

  private fun handleRootIsArrayCollision(
    node: ArrayStructuredModelItem,
    fullPath: List<PathItem>,
    value: String?,
  ) {
    replaceArrayWithObjectOnCollision(node)
    addToContent(model!!, fullPath, fullPath, value)
  }

  private fun replaceArrayWithObjectOnCollision(node: ArrayStructuredModelItem) {
    val replacingObjectItem = ObjectStructuredModelItem(node.parent, "[${node.key}]")
    node.forEach { (key, value) ->
      replacingObjectItem["[$key]"] = value
    }

    if (node.parent == null) {
      model = replacingObjectItem
      return
    }

    (node.key as? Int)?.let {
      @Suppress("UNCHECKED_CAST")
      (node.parent as MutableMap<Int, Any>).put(it, replacingObjectItem)
      return
    }

    (node.key as? String)?.let {
      @Suppress("UNCHECKED_CAST")
      (node.parent as MutableMap<String, Any>).put(it, replacingObjectItem)
      return
    }
  }
}
