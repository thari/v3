// #Sireum
/*
 Copyright (c) 2017, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sireum.lang.tipe

import org.sireum._
import org.sireum.lang.symbol.Resolver._
import org.sireum.lang.util._
import org.sireum.lang.{ast => AST}

object TypeHierarchy {

  @datatype class TypeName(t: AST.Typed.Name) {
    override def hash: Z = {
      return t.ids.hash
    }

    def isEqual(other: TypeName): B = {
      return t.ids == other.t.ids
    }
  }

  val NothingTypeName: TypeName = TypeName(AST.Typed.nothing)

  @pure def typedInfo(info: TypeInfo): AST.Typed.Name = {
    @pure def typedParam(tp: AST.TypeParam): AST.Typed = {
      return AST.Typed.Name(ISZ(tp.id.value), ISZ())
    }

    info match {
      case info: TypeInfo.SubZ => return AST.Typed.Name(info.name, ISZ())
      case info: TypeInfo.Enum => return AST.Typed.Name(info.name :+ "Type", ISZ())
      case info: TypeInfo.Sig =>
        val args = info.ast.typeParams.map(typedParam)
        return AST.Typed.Name(info.name, args)
      case info: TypeInfo.AbstractDatatype =>
        val args = info.ast.typeParams.map(typedParam)
        return AST.Typed.Name(info.name, args)
      case info: TypeInfo.TypeAlias =>
        val args = info.ast.typeParams.map(typedParam)
        return AST.Typed.Name(info.name, args)
      case _: TypeInfo.TypeVar => halt("Infeasible")
    }
  }

  def build(init: TypeHierarchy, reporter: Reporter): TypeHierarchy = {
    val typeMap = init.typeMap

    def resolveType(scope: Scope, t: AST.Type): AST.Typed = {
      t match {
        case t: AST.Type.Named =>
          var name = AST.Util.ids2strings(t.name.ids)
          scope.resolveType(typeMap, name) match {
            case Some(ti) => name = ti.name
            case _ => reporter.error(t.name.attr.posOpt, resolverKind, s"Could not resolve type named '${st"${(name, ".")}".render}'.")
          }
          val args: ISZ[AST.Typed] = {
            var as = ISZ[AST.Typed]()
            for (ta <- t.typeArgs) {
              val tas = resolveType(scope, ta)
              as = as :+ tas
            }
            as
          }
          AST.Typed.Name(name, args)
        case t: AST.Type.Tuple =>
          val ts: ISZ[AST.Typed] = {
            var as = ISZ[AST.Typed]()
            for (a <- t.args) {
              val ta = resolveType(scope, a)
              as = as :+ ta
            }
            as
          }
          AST.Typed.Tuple(ts)
        case t: AST.Type.Fun =>
          val ts: ISZ[AST.Typed] = {
            var as = ISZ[AST.Typed]()
            for (a <- t.args) {
              val ta = resolveType(scope, a)
              as = as :+ ta
            }
            as
          }
          val rt = resolveType(scope, t.ret)
          AST.Typed.Fun(t.isPure || rt.isPureFun, t.isByName, ts, rt)
      }
    }

    def resolveTypeNameds(posOpt: Option[AST.PosInfo],
                          scope: Scope,
                          ts: ISZ[AST.Type.Named]): ISZ[AST.Typed.Name] = {
      var r = ISZ[AST.Typed.Name]()
      for (t <- ts) {
        val typed = resolveType(scope, t)
        typed match {
          case typed: AST.Typed.Name => r = r :+ typed
          case _ => reporter.error(posOpt, resolverKind, "Expected a named type.")
        }
      }
      return r
    }

    val zName: QName = ISZ("org", "sireum", "Z")
    var r = init

    def resolveAlias(info: TypeInfo.TypeAlias, seen: HashSet[QName]): AST.Typed = {
      if (seen.contains(info.name)) {
        reporter.error(info.posOpt, resolverKind, st"Type alias ${(info.name, ".")} is cyclic.".render)
        return AST.Typed.Name(info.name, ISZ())
      }
      val typed = typedInfo(info)
      r.aliases.get(TypeHierarchy.TypeName(typed)) match {
        case Some(rt) => return rt
        case _ =>
      }
      val scope = typeParamsScope(info.ast.typeParams, info.scope, reporter)
      val t = resolveType(scope, info.ast.tipe)
      t match {
        case t: AST.Typed.Name =>
          typeMap.get(t.ids) match {
            case Some(ti: TypeInfo.TypeAlias) => resolveAlias(ti, seen.add(info.name))
            case _ =>
          }
        case _ =>
      }
      r = r(aliases = r.aliases.put(TypeHierarchy.TypeName(typed), t))
      return t
    }

    if (!typeMap.contains(zName)) {
      reporter.error(None(), resolverKind, "Could not find Z type.")
      return r
    }
    for (info <- typeMap.values) {
      info match {
        case _: TypeInfo.SubZ =>
          val typed = typedInfo(info)
          val ttn = TypeHierarchy.TypeName(typed)
          r = r(poset = r.poset.addNode(ttn))
        case _: TypeInfo.Enum =>
          val typed = typedInfo(info)
          val ttn = TypeHierarchy.TypeName(typed)
          r = r(poset = r.poset.addNode(ttn))
        case info: TypeInfo.Sig =>
          if (!info.outlined) {
            val typed = typedInfo(info)
            val ttn = TypeHierarchy.TypeName(typed)
            val scope = typeParamsScope(info.ast.typeParams, info.scope, reporter)
            val parents = resolveTypeNameds(info.posOpt, scope, info.ast.parents)
            var parentTypeNames = ISZ[TypeHierarchy.TypeName]()
            for (p <- parents) {
              val ptn = TypeHierarchy.TypeName(p)
              parentTypeNames = parentTypeNames :+ ptn
            }
            r = r(poset = r.poset.addParents(ttn, parentTypeNames))
          }
        case info: TypeInfo.AbstractDatatype =>
          if (!info.outlined) {
            val typed = typedInfo(info)
            val ttn = TypeHierarchy.TypeName(typed)
            val scope = typeParamsScope(info.ast.typeParams, info.scope, reporter)
            val parents = resolveTypeNameds(info.posOpt, scope, info.ast.parents)
            var parentTypeNames = ISZ[TypeHierarchy.TypeName]()
            for (p <- parents) {
              val ptn = TypeHierarchy.TypeName(p)
              parentTypeNames = parentTypeNames :+ ptn
            }
            r = r(poset = r.poset.addParents(ttn, parentTypeNames))
          }
        case info: TypeInfo.TypeAlias => resolveAlias(info, HashSet.empty)
        case _: TypeInfo.TypeVar => halt("Infeasible")
      }
    }
    if (reporter.hasIssue) {
      return r
    }
    r.checkCyclic(reporter)
    return r
  }

}

@datatype class TypeHierarchy(nameMap: NameMap,
                              typeMap: TypeMap,
                              poset: Poset[TypeHierarchy.TypeName],
                              aliases: HashMap[TypeHierarchy.TypeName, AST.Typed]) {

  @pure def rootTypes: ISZ[AST.Typed.Name] = {
    var r = ISZ[AST.Typed.Name]()
    for (ps <- poset.parents.entries) {
      if (ps._2.isEmpty) {
        r = r :+ ps._1.t
      }
    }
    return r
  }

  @pure def rootTypeNames(): ISZ[QName] = {
    var r = ISZ[QName]()
    for (p <- poset.parents.entries) {
      if (p._2.isEmpty) {
        r = r :+ p._1.t.ids
      }
    }
    return r
  }

  @pure def lub(ts: ISZ[AST.Typed]): Option[AST.Typed] = {
    val types: ISZ[AST.Typed] =
      for (t <- ts if AST.Typed.nothing != t) yield t
    types.size match {
      case z"0" => return if (ts.isEmpty) None() else Some(ts(0))
      case z"1" => return Some(types(0))
      case _ =>
    }

    var typeNames = ISZ[AST.Typed.Name]()
    val first = types(0)
    var i = 0
    val size = types.size
    while (i < size) {
      types(i) match {
        case t: AST.Typed.Name =>
          typeNames = typeNames :+ t
        case t =>
          if (first != t) {
            return None()
          }
      }
      i = i + 1
    }

    if (typeNames.size < 2) {
      return Some(first)
    }

    val tns = typeNames.map(tn => TypeHierarchy.TypeName(tn))
    poset.lub(tns) match {
      case Some(lub) =>
        val tn = typeNames(0)
        val ancestors: ISZ[AST.Typed.Name] = typeMap.get(tn.ids) match {
          case Some(info: TypeInfo.Sig) => info.ancestors
          case Some(info: TypeInfo.AbstractDatatype) => info.ancestors
          case _ => halt("Unexpected situation while type checking assign-exp.")
        }
        val lubName = lub.t.ids
        var commonType = tn
        var found = F
        for (ancestor <- ancestors if !found) {
          if (ancestor.ids == lubName) {
            commonType = ancestor
            found = T
          }
        }
        for (typeName <- typeNames) {
          if (!isSubType(typeName, commonType)) {
            return None()
          }
        }
        return Some(commonType)
      case _ => return None()
    }
  }

  def dealiasInit(t: AST.Typed.Name, reporter: Reporter): Option[AST.Typed.Name] = {
    aliases.get(TypeHierarchy.TypeName(t)) match {
      case Some(t2: AST.Typed.Name) =>
        val r = dealiasInit(t2, reporter)
        return r
      case Some(_) =>
        reporter.error(None(), resolverKind, st"Expected a named type in type hiearchy but ${(t.ids, ".")} is not.".render)
        return None()
      case _ => return Some(t)
    }
  }

  def checkCyclic(reporter: Reporter): Unit = {
    var cache = HashMap.empty[TypeHierarchy.TypeName, HashSet[TypeHierarchy.TypeName]]
    for (t <- rootTypes) {
      val r = poset.descendantsCache(TypeHierarchy.TypeName(t), cache)
      cache = r._2
    }
    for (kv <- cache.entries) {
      val k = kv._1
      val v = kv._2
      if (v.contains(k)) {
        reporter.error(None(), resolverKind, st"Cyclic type hierarchy involving ${(k.t.ids, ".")}.".render)
      }
    }
  }

  def typed(scope: Scope,
            tipe: AST.Type,
            reporter: Reporter): Option[AST.Type] = {
    tipe match {
      case tipe: AST.Type.Named =>
        var newTypeArgs = ISZ[AST.Type]()
        var argTypes = ISZ[AST.Typed]()
        var hasError = F
        for (arg <- tipe.typeArgs) {
          val targOpt = typed(scope, arg, reporter)
          targOpt match {
            case Some(targ) if targ.typedOpt.nonEmpty =>
              newTypeArgs = newTypeArgs :+ targ
              argTypes = argTypes :+ targ.typedOpt.get
            case _ => hasError = T
          }
        }
        if (hasError) {
          return None[AST.Type]()
        }
        val name = AST.Util.ids2strings(tipe.name.ids)
        scope.resolveType(typeMap, name) match {
          case Some(ti: TypeInfo.TypeAlias) =>
            val tparamSize = ti.ast.typeParams.size
            if (newTypeArgs.size != tparamSize) {
              reporter.error(tipe.posOpt, TypeChecker.typeCheckerKind,
                st"Type alias ${(name, ".")} requires $tparamSize type arguments but ${newTypeArgs.size} supplied.".render)
              return None()
            }
            val tpe = dealias(AST.Typed.Name(ti.name, argTypes), tipe.posOpt, reporter)
            return Some(tipe(typeArgs = newTypeArgs, attr = tipe.attr(typedOpt = Some(tpe))))
          case Some(ti: TypeInfo.TypeVar) =>
            if (newTypeArgs.nonEmpty) {
              reporter.error(tipe.posOpt, TypeChecker.typeCheckerKind,
                s"Type variable ${name(0)} does not accept type arguments.")
              return None()
            }
            return Some(tipe(attr = tipe.attr(typedOpt = Some(AST.Typed.TypeVar(ti.name(0))))))
          case Some(ti) =>
            val p: (String, Z) = ti match {
              case ti: TypeInfo.SubZ => (if (ti.ast.isBitVector) "@bits" else "@range", 0)
              case _: TypeInfo.Enum => ("@enum", 0)
              case ti: TypeInfo.Sig => (if (ti.ast.isExt) "@ext" else if (ti.ast.isImmutable) "@sig" else "@msig", ti.ast.typeParams.size)
              case ti: TypeInfo.AbstractDatatype => (if (ti.ast.isDatatype) "@datatype" else "@record", ti.ast.typeParams.size)
              case _ => halt("Infeasible")
            }
            if (newTypeArgs.size != p._2) {
              if (p._2 == 0) {
                reporter.error(tipe.posOpt, TypeChecker.typeCheckerKind,
                  st"Slang ${p._1} ${(name, ".")} does not accept type arguments.".render)
              } else {
                reporter.error(tipe.posOpt, TypeChecker.typeCheckerKind,
                  st"Slang ${p._1} ${(name, ".")} requires ${p._2} type arguments but ${newTypeArgs.size} is supplied.".render)
              }
              return None()
            }
            return Some(tipe(typeArgs = newTypeArgs, attr = tipe.attr(typedOpt = Some(AST.Typed.Name(ti.name, argTypes)))))
          case _ =>
            if (name.size == 1 && newTypeArgs.isEmpty && name(0) == "Unit") {
              return Some(tipe(attr = tipe.attr(typedOpt = Some(AST.Typed.unit))))
            } else {
              reporter.error(tipe.posOpt, TypeChecker.typeCheckerKind, st"Could not find a type named ${(name, ".")}.".render)
            }
            return None()
        }
      case tipe: AST.Type.Tuple =>
        var newArgs = ISZ[AST.Type]()
        var esTypes = ISZ[AST.Typed]()
        var hasError = F
        for (arg <- tipe.args) {
          val targOpt = typed(scope, arg, reporter)
          targOpt match {
            case Some(targ) if targ.typedOpt.nonEmpty =>
              newArgs = newArgs :+ targ
              esTypes = esTypes :+ targ.typedOpt.get
            case _ => hasError = T
          }
        }
        if (hasError) {
          return None[AST.Type]()
        } else {
          return Some(tipe(args = newArgs, attr = tipe.attr(typedOpt = Some(AST.Typed.Tuple(esTypes)))))
        }
      case tipe: AST.Type.Fun =>
        var paramTypes = ISZ[AST.Typed]()
        var newArgs = ISZ[AST.Type]()
        var hasError = F
        for (arg <- tipe.args) {
          val targOpt = typed(scope, arg, reporter)
          targOpt match {
            case Some(targ) if targ.typedOpt.nonEmpty =>
              newArgs = newArgs :+ targ
              paramTypes = paramTypes :+ targ.typedOpt.get
            case _ => hasError = T
          }
        }
        val newRetOpt = typed(scope, tipe.ret, reporter)
        newRetOpt match {
          case Some(newRet) if !hasError && newRet.typedOpt.nonEmpty =>
            val retType = newRet.typedOpt.get
            return Some(tipe(args = newArgs, ret = newRet, attr = tipe.attr(typedOpt = Some(
              AST.Typed.Fun(tipe.isPure || retType.isPureFun, tipe.isByName, paramTypes, retType)))))
          case _ => return None[AST.Type]()
        }
    }
  }

  def applyTypeAlias(typed: AST.Typed.Name,
                     ti: TypeInfo.TypeAlias,
                     posOpt: Option[AST.PosInfo],
                     reporter: Reporter): Option[AST.Typed] = {
    val tm = typeParamMap(ti.ast.typeParams, reporter)
    val scope = localTypeScope(tm.map, ti.scope)
    val tipeOpt = this.typed(scope, ti.ast.tipe, reporter)
    tipeOpt match {
      case Some(tipe) if tipe.typedOpt.nonEmpty =>
        val substMapOpt = TypeChecker.buildTypeSubstMap(ti.name, posOpt, ti.ast.typeParams, typed.args, reporter)
        substMapOpt match {
          case Some(substMap) => return Some(tipe.typedOpt.get.subst(substMap))
          case _ => return None()
        }
      case _ => return None()
    }
  }

  def dealias(typed: AST.Typed, posOpt: Option[AST.PosInfo], reporter: Reporter): AST.Typed = {
    def dealiasOpt(t: AST.Typed): Option[AST.Typed] = {
      t match {
        case t: AST.Typed.Name =>
          var newArgs = ISZ[AST.Typed]()
          var changed = F
          for (arg <- t.args) {
            val newArgOpt = dealiasOpt(arg)
            newArgOpt match {
              case Some(newArg) =>
                newArgs = newArgs :+ newArg
                changed = T
              case _ => newArgs = newArgs :+ arg
            }
          }
          val typed2: AST.Typed.Name = if (changed) t(args = newArgs) else t
          typeMap.get(t.ids) match {
            case Some(ti: TypeInfo.TypeAlias) =>
              val rOpt = applyTypeAlias(typed2, ti, posOpt, reporter)
              rOpt match {
                case Some(r) =>
                  val r2 = dealias(r, posOpt, reporter)
                  return Some(r2)
                case _ => return None()
              }
            case _ =>
              return if (changed) Some(typed2) else None()
          }
        case t: AST.Typed.Tuple =>
          var newArgs = ISZ[AST.Typed]()
          var changed = F
          for (arg <- t.args) {
            val newArgOpt = dealiasOpt(arg)
            newArgOpt match {
              case Some(newArg) =>
                newArgs = newArgs :+ newArg
                changed = T
              case _ =>
                newArgs = newArgs :+ arg
            }
          }
          return if (changed) Some(t(args = newArgs)) else None()
        case t: AST.Typed.Fun =>
          var newArgs = ISZ[AST.Typed]()
          var changed = F
          for (arg <- t.args) {
            val newArgOpt = dealiasOpt(arg)
            newArgOpt match {
              case Some(newArg) =>
                newArgs = newArgs :+ newArg
                changed = T
              case _ =>
                newArgs = newArgs :+ arg
            }
          }
          val newRetOpt = dealiasOpt(t.ret)
          val newRet = newRetOpt.getOrElse(t.ret)
          changed = changed || newRetOpt.nonEmpty
          return if (changed) Some(t(args = newArgs, ret = newRet)) else None()
        case _: AST.Typed.TypeVar => return None()
        case _: AST.Typed.Enum => return None()
        case _: AST.Typed.Method => return None()
        case _: AST.Typed.Methods => return None()
        case _: AST.Typed.Object => return None()
        case _: AST.Typed.Package => return None()
      }
    }

    val rOpt = dealiasOpt(typed)
    return rOpt.getOrElse(typed)
  }

  @pure def isRefinement(t1: AST.Typed, t2: AST.Typed): B = {
    (t1, t2) match {
      case (t1: AST.Typed.Fun, t2: AST.Typed.Fun) =>
        if (t1.args.size != t2.args.size) {
          return F
        }
        for (i <- z"0" until t1.args.size) {
          if (!isSubType(t2.args(i), t1.args(i))) {
            return F
          }
        }
        return isSubType(t1.ret, t2.ret)
      case (t1: AST.Typed.Method, t2: AST.Typed.Method) =>
        return isRefinement(t1.tpe, t2.tpe)
      case _ => isSubType(t1, t2)
    }
  }

  @pure def isSubType(t1: AST.Typed, t2: AST.Typed): B = {
    if (AST.Typed.nothing == t1) {
      return T
    }
    (t1, t2) match {
      case (t1: AST.Typed.Name, t2: AST.Typed.Name) =>
        if (t1.args.size != t2.args.size) {
          return F
        }
        for (i <- z"0" until t1.args.size) {
          if (t1.args(i) != t2.args(i)) {
            return F
          }
        }
        if (t1.ids == t2.ids) {
          return T
        }
        return poset.ancestorsOf(TypeHierarchy.TypeName(t1)).contains(TypeHierarchy.TypeName(t2))
      case _ => return t1 == t2
    }
  }

  @pure def isCompatible(t1: AST.Typed, t2: AST.Typed): B = {
    return isSubType(t1, t2) || isSubType(t2, t1)
  }
}
