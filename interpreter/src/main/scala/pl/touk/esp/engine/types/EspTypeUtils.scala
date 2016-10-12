package pl.touk.esp.engine.types

import java.lang.reflect.{Method, Modifier, Type}

import cats.Eval
import cats.data.StateT
import pl.touk.esp.engine.definition.DefinitionExtractor.{ClazzRef, PlainClazzDefinition}
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl

import scala.concurrent.Future
import scala.util.Try

object EspTypeUtils {

  private object ScalaCaseClassStub {
    case class DumpCaseClass()
    object DumpCaseClass
  }
  private val blackilistedMethods: Set[String] = {
    (methodNames(classOf[ScalaCaseClassStub.DumpCaseClass]) ++
      methodNames(ScalaCaseClassStub.DumpCaseClass.getClass)).toSet
  }

  private val primitiveTypesSimpleNames = Set(
    "void", "boolean", "int", "long", "float", "double", "byte", "short", "char"
  )

  private val baseClazzPackagePrefix = Set("java", "scala")

  private val blacklistedClazzPackagePrefix = Set(
    "scala.collection", "scala.Function", "scala.xml",
    "javax.xml", "java.util",
    "cats", "argonaut", "dispatch",
    "org.apache.flink.api.common.typeinfo.TypeInformation"
  )

  private def methodNames(clazz: Class[_]): List[String] = {
    clazz.getMethods.map(_.getName).toList
  }

  def clazzAndItsChildrenDefinition(clazzes: List[Class[_]]): List[PlainClazzDefinition] = {
    clazzes.flatMap(clazzAndItsChildrenDefinition).distinct
  }

  def clazzAndItsChildrenDefinition(clazz: Class[_]): List[PlainClazzDefinition] = {
    val result = if (clazz.isPrimitive || baseClazzPackagePrefix.exists(clazz.getName.startsWith)) {
      List(clazzDefinition(clazz))
    } else {
      val mainClazzDefinition = clazzDefinition(clazz)
      val recursiveClazzes = mainClazzDefinition.methods.values.toList
        .filter(m => !primitiveTypesSimpleNames.contains(m.refClazzName) && m.refClazzName != clazz.getName)
        .filter(m => !blacklistedClazzPackagePrefix.exists(m.refClazzName.startsWith))
        .map(_.refClazzName).distinct
        .flatMap(m => clazzAndItsChildrenDefinition(Class.forName(m)))
      mainClazzDefinition :: recursiveClazzes
    }
    result.distinct
  }

  private def clazzDefinition(clazz: Class[_]): PlainClazzDefinition = {
    PlainClazzDefinition(ClazzRef(clazz), getDeclaredMethods(clazz))
  }

  private def getDeclaredMethods(clazz: Class[_]): Map[String, ClazzRef] = {
    val interestingMethods = clazz.getDeclaredMethods.toList.filter( m =>
      !blackilistedMethods.contains(m.getName) && Modifier.isPublic(m.getModifiers) && !m.getName.contains("$")
    )
    val res = interestingMethods.map { method =>
      method.getName -> ClazzRef(getGenericMethodType(method).getOrElse(method.getReturnType))
    }.toMap
    res
  }

  def getGenericMethodType(m: Method): Option[Class[_]] = {
    val genericReturnType = m.getGenericReturnType
    val hasGenericReturnType = genericReturnType.isInstanceOf[ParameterizedTypeImpl]
    if (hasGenericReturnType) inferGenericMonadType(genericReturnType)
    else None
  }

  //TODO to nie dziala poprawnie np. dla primitywow i skomplikowanych hierarchii, ale na razie chyba wystarczy
  //http://docs.oracle.com/javase/8/docs/api/java/lang/reflect/ParameterizedType.html#getActualTypeArguments--
  private def inferGenericMonadType(genericReturnType: Type): Option[Class[_]] = {
    val genericMethodType = genericReturnType.asInstanceOf[ParameterizedTypeImpl]
    if (classOf[StateT[Eval, _, _]].isAssignableFrom(genericMethodType.getRawType)) {
      val returnType = genericMethodType.getActualTypeArguments.apply(2) // bo StateT[Eval, S, A]
      extractClass(returnType)
    }
    else if (classOf[Future[_]].isAssignableFrom(genericMethodType.getRawType)) {
      val futureGenericType = genericMethodType.getActualTypeArguments.apply(0)
      extractClass(futureGenericType)
    }
    else None
  }

  private def extractClass(futureGenericType: Type): Option[Class[_]] = {
    futureGenericType match {
      case t: Class[_] => Some(t)
      case t: ParameterizedTypeImpl => Some(t.getRawType)
      case t => None
    }
  }
}
