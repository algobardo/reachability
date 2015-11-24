/*
 * Copyright (c) 2015,
 * Ilya Sergey, Christopher Earl, Matthew Might and David Van Horn
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project "Reachability" nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ucombinator.scheme.cfa.gc

import org.ucombinator.scheme.cfa.cesk._
import org.ucombinator.gc.GCInterface

/**
 * @author ilya
 */

trait SchemeGarbageCollector extends StateSpace with GCInterface {

  def gc(c: ControlState, frames: Kont): ControlState = c match {
    case ErrorState(_, _) | PFinal(_) => c
    case PState(e, rho, s, kptr) => {
      val alive = reachable(c, frames)
      val cleanStore = s.filter {
        case (a, _) => alive.contains(a)
      }
      PState(e, rho, cleanStore, kptr)
    }
  }

  def reachable(c: ControlState, frames: Kont): Set[Addr] = {
    val rootAddresses: Set[Addr] = rootAddr(c, frames)
    c match {
      case ErrorState(_, _) | PFinal(_) => Set.empty
      case PState(_, _, s, kptr) => {
        val result: Set[Addr] = collectAdjacent(rootAddresses, s)
        result

        if (printGCDebug) {
          val original = s.keys.toSet
          val delta = original -- result
          if (!delta.isEmpty) {
            println("Original store size: " + original.size + "")
            println("Result size: " + result.size + "")
            println("Store delta (size " + delta.size + "):")
            println(delta)
            println()
          }
        }

        result
      }
    }
  }

  /**
   * Get addresses from a stack
   *
   * @param f a frame with environment
   * @return
   */
  def fetchAddressesFromFrame(f: Frame) = f match {
    case LetFrame(_, _, rho) => rho.values
    case IfFrame(_, _, rho) => rho.values
    case SeqFrame(_, _, rho, _) => rho.values
    case _ => Set()
  }

  def collectAdjacent(previousAddrs: Set[Addr], store: Store): Set[Addr] = {

    val filteredStore = store.filter {
      // only addresses in previousAddrs
      case (a, vals) => previousAddrs.contains(a)
    }

    val relevantValues = filteredStore.flatMap {
      // flatten values
      case (a, vals) => vals
    }

    val relevantClosures = relevantValues.filter {
      // take closures only
      case Clo(lam, rho) => true
      case _ => false
    }.toSet


    val relevantEnvs = relevantClosures.map {
      (v: Val) => v match {
        // take environments
        case Clo(_, rho) => rho
      }
    }
    val newAddresses: Set[Addr] = relevantEnvs.flatMap {
      // take ranges of all environments
      // from closures in store
      rho => rho.values
    }

    if (newAddresses subsetOf previousAddrs) {
      previousAddrs
    } else {
      collectAdjacent(newAddresses ++ previousAddrs, store)
    }
  }

}

